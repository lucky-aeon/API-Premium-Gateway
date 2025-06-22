package org.xhy.gateway.domain.apiinstance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.gateway.domain.apiinstance.command.InstanceSelectionCommand;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.apiinstance.entity.LoadBalancingType;
import org.xhy.gateway.domain.apiinstance.repository.ApiInstanceRepository;
import org.xhy.gateway.domain.apiinstance.strategy.LoadBalancingStrategy;
import org.xhy.gateway.domain.apiinstance.strategy.LoadBalancingStrategyFactory;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;
import org.xhy.gateway.infrastructure.exception.BusinessException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * API实例选择领域服务
 * 专注于API实例选择逻辑，不依赖其他领域
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class ApiInstanceSelectionDomainService {

    private static final Logger logger = LoggerFactory.getLogger(ApiInstanceSelectionDomainService.class);

    private final ApiInstanceRepository apiInstanceRepository;
    private final LoadBalancingStrategyFactory strategyFactory;
    private final AffinityAwareStrategyDecorator affinityDecorator;

    public ApiInstanceSelectionDomainService(ApiInstanceRepository apiInstanceRepository,
                                           LoadBalancingStrategyFactory strategyFactory,
                                           AffinityAwareStrategyDecorator affinityDecorator) {
        this.apiInstanceRepository = apiInstanceRepository;
        this.strategyFactory = strategyFactory;
        this.affinityDecorator = affinityDecorator;
    }

    /**
     * 查找候选实例
     * 支持通过apiIdentifier或businessId查找实例
     */
    public List<ApiInstanceEntity> findCandidateInstances(InstanceSelectionCommand command) {
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
     * 过滤掉被熔断的实例
     */
    public List<ApiInstanceEntity> filterHealthyInstances(List<ApiInstanceEntity> candidates, 
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

    /**
     * 使用策略选择最佳实例
     * 使用用户指定的负载均衡策略，支持亲和性绑定
     * 
     * @param healthyInstances 健康的候选实例
     * @param metricsMap 实例指标数据
     * @param command 实例选择命令对象
     * @return 选中的实例
     */
    public ApiInstanceEntity selectInstanceWithStrategy(List<ApiInstanceEntity> healthyInstances, 
                                                       Map<String, InstanceMetricsEntity> metricsMap,
                                                       InstanceSelectionCommand command) {
        logger.info("开始使用策略选择最佳API实例: 候选实例数={}, 策略={}", 
                healthyInstances.size(), command.getLoadBalancingType());

        if (healthyInstances.isEmpty()) {
            throw new BusinessException("NO_HEALTHY_INSTANCE", "没有健康的API实例可供选择");
        }

        // 使用亲和性感知的策略选择实例
        LoadBalancingStrategy strategy = strategyFactory.getStrategy(LoadBalancingType.ROUND_ROBIN);
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
} 