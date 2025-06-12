package org.xhy.gateway.domain.apiinstance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.gateway.domain.apiinstance.command.InstanceSelectionCommand;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.apiinstance.repository.ApiInstanceRepository;
import org.xhy.gateway.domain.apiinstance.strategy.LoadBalancingStrategy;
import org.xhy.gateway.domain.apiinstance.strategy.LoadBalancingStrategyFactory;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;
import org.xhy.gateway.domain.metrics.repository.MetricsRepository;
import org.xhy.gateway.domain.project.service.ProjectDomainService;
import org.xhy.gateway.infrastructure.exception.BusinessException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.xhy.gateway.domain.apiinstance.service.SelectionConstants.*;

/**
 * API实例选择领域服务
 * 使用策略模式实现智能调度算法，支持亲和性绑定
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class ApiInstanceSelectionDomainService {

    private static final Logger logger = LoggerFactory.getLogger(ApiInstanceSelectionDomainService.class);

    private final ApiInstanceRepository apiInstanceRepository;
    private final MetricsRepository metricsRepository;
    private final ProjectDomainService projectDomainService;
    private final LoadBalancingStrategyFactory strategyFactory;
    private final AffinityAwareStrategyDecorator affinityDecorator;

    public ApiInstanceSelectionDomainService(ApiInstanceRepository apiInstanceRepository,
                                           MetricsRepository metricsRepository,
                                           ProjectDomainService projectDomainService,
                                           LoadBalancingStrategyFactory strategyFactory,
                                           AffinityAwareStrategyDecorator affinityDecorator) {
        this.apiInstanceRepository = apiInstanceRepository;
        this.metricsRepository = metricsRepository;
        this.projectDomainService = projectDomainService;
        this.strategyFactory = strategyFactory;
        this.affinityDecorator = affinityDecorator;
    }

    /**
     * 选择最佳API实例
     * 使用用户指定的负载均衡策略，支持亲和性绑定
     * 
     * @param command 实例选择命令对象
     * @return 选中的实例
     */
    public ApiInstanceEntity selectBestInstance(InstanceSelectionCommand command) {
        logger.info("开始选择最佳API实例: {}", command);

        // 1. 验证项目存在
        projectDomainService.validateProjectExists(command.getProjectId());

        // 2. 查找候选实例
        List<ApiInstanceEntity> candidates = findCandidateInstances(command);
        if (candidates.isEmpty()) {
            throw new BusinessException("NO_AVAILABLE_INSTANCE", 
                    String.format("没有可用的API实例: projectId=%s, apiIdentifier=%s, apiType=%s", 
                            command.getProjectId(), command.getApiIdentifier(), command.getApiType()));
        }

        // 3. 获取实例指标
        Map<String, InstanceMetricsEntity> metricsMap = getInstanceMetrics(candidates);

        // 4. 过滤掉被熔断的实例
        List<ApiInstanceEntity> healthyInstances = filterHealthyInstances(candidates, metricsMap);
        if (healthyInstances.isEmpty()) {
            throw new BusinessException("NO_HEALTHY_INSTANCE", "所有API实例都不可用或被熔断");
        }

        // 5. 使用亲和性感知的策略选择实例
        LoadBalancingStrategy strategy = strategyFactory.getStrategy(command.getLoadBalancingType());
        ApiInstanceEntity selected = affinityDecorator.selectInstanceWithAffinity(
            healthyInstances, 
            metricsMap, 
            strategy, 
            command.getAffinityContext()
        );

        if (command.hasAffinityRequirement()) {
            logger.info("选择API实例成功（含亲和性）: businessId={}, instanceId={}, strategy={}, affinity={}", 
                    selected.getBusinessId(), selected.getId(), command.getLoadBalancingType(), 
                    command.getAffinityContext().getBindingKey());
        } else {
            logger.info("选择API实例成功: businessId={}, instanceId={}, strategy={}", 
                    selected.getBusinessId(), selected.getId(), command.getLoadBalancingType());
        }
        
        return selected;
    }

    /**
     * 查找候选实例
     * 支持通过apiIdentifier或businessId查找实例
     */
    private List<ApiInstanceEntity> findCandidateInstances(InstanceSelectionCommand command) {
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiInstanceEntity::getProjectId, command.getProjectId())
                   .eq(ApiInstanceEntity::getApiType, ApiType.fromCode(command.getApiType()))
                   .eq(ApiInstanceEntity::getStatus, ApiInstanceStatus.ACTIVE);
        
        // 优先通过apiIdentifier查找，如果没有找到则尝试通过businessId查找
        queryWrapper.and(wrapper -> wrapper
                .eq(ApiInstanceEntity::getApiIdentifier, command.getApiIdentifier())
                .or()
                .eq(ApiInstanceEntity::getBusinessId, command.getApiIdentifier())
        );
        
        // 如果指定了用户ID，则过滤用户
        if (command.getUserId() != null && !command.getUserId().trim().isEmpty()) {
            queryWrapper.eq(ApiInstanceEntity::getUserId, command.getUserId());
        }

        List<ApiInstanceEntity> candidates = apiInstanceRepository.selectList(queryWrapper);
        
        logger.debug("查找候选实例: projectId={}, apiIdentifier={}, apiType={}, 找到{}个候选实例", 
                command.getProjectId(), command.getApiIdentifier(), command.getApiType(), candidates.size());
        
        return candidates;
    }

    /**
     * 获取实例指标数据
     */
    private Map<String, InstanceMetricsEntity> getInstanceMetrics(List<ApiInstanceEntity> instances) {
        List<String> instanceIds = instances.stream()
                .map(ApiInstanceEntity::getId)
                .collect(Collectors.toList());

        // 获取最近的指标数据（最近5分钟内）
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(SHORT_TERM_WINDOW_MINUTES);
        
        LambdaQueryWrapper<InstanceMetricsEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(InstanceMetricsEntity::getRegistryId, instanceIds)
                   .ge(InstanceMetricsEntity::getTimestampWindow, cutoffTime)
                   .orderByDesc(InstanceMetricsEntity::getTimestampWindow);

        List<InstanceMetricsEntity> metricsList = metricsRepository.selectList(queryWrapper);
        
        // 聚合同一实例的指标数据（取最新的）
        return metricsList.stream()
                .collect(Collectors.toMap(
                        InstanceMetricsEntity::getRegistryId,
                        metrics -> metrics,
                        (existing, replacement) -> 
                                existing.getTimestampWindow().isAfter(replacement.getTimestampWindow()) 
                                        ? existing : replacement
                ));
    }

    /**
     * 过滤掉被熔断的实例
     */
    private List<ApiInstanceEntity> filterHealthyInstances(List<ApiInstanceEntity> candidates, 
                                                          Map<String, InstanceMetricsEntity> metricsMap) {
        return candidates.stream()
                .filter(instance -> {
                    InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
                    if (metrics != null && metrics.isCircuitBreakerOpen()) {
                        logger.debug("实例被熔断，过滤掉: instanceId={}, businessId={}", 
                                instance.getId(), instance.getBusinessId());
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }
} 