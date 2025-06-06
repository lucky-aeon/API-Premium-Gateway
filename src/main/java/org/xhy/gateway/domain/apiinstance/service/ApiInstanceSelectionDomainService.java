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
 * 实现核心的智能调度算法
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

    public ApiInstanceSelectionDomainService(ApiInstanceRepository apiInstanceRepository,
                                           MetricsRepository metricsRepository,
                                           ProjectDomainService projectDomainService) {
        this.apiInstanceRepository = apiInstanceRepository;
        this.metricsRepository = metricsRepository;
        this.projectDomainService = projectDomainService;
    }

    /**
     * 选择最佳API实例
     * 核心调度算法实现
     * 
     * @param command 实例选择命令对象
     * @return 选中的实例业务ID
     */
    public String selectBestInstance(InstanceSelectionCommand command) {
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

        // 4. 执行综合评分算法
        ApiInstanceEntity selected = selectByComprehensiveScore(candidates, metricsMap);

        logger.info("选择API实例成功: businessId={}, instanceId={}", 
                selected.getBusinessId(), selected.getId());
        return selected.getBusinessId();
    }

    /**
     * 查找候选实例
     */
    private List<ApiInstanceEntity> findCandidateInstances(InstanceSelectionCommand command) {
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiInstanceEntity::getProjectId, command.getProjectId())
                   .eq(ApiInstanceEntity::getApiIdentifier, command.getApiIdentifier())
                   .eq(ApiInstanceEntity::getApiType, ApiType.fromCode(command.getApiType()))
                   .eq(ApiInstanceEntity::getStatus, ApiInstanceStatus.ACTIVE);
        
        // 如果指定了用户ID，则过滤用户
        if (command.getUserId() != null && !command.getUserId().trim().isEmpty()) {
            queryWrapper.eq(ApiInstanceEntity::getUserId, command.getUserId());
        }

        return apiInstanceRepository.selectList(queryWrapper);
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
     * 综合评分算法选择实例
     */
    private ApiInstanceEntity selectByComprehensiveScore(List<ApiInstanceEntity> candidates, 
                                                        Map<String, InstanceMetricsEntity> metricsMap) {
        
        ApiInstanceEntity bestInstance = null;
        double bestScore = -1.0;

        for (ApiInstanceEntity instance : candidates) {
            InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
            
            // 如果实例被熔断，跳过
            if (metrics != null && metrics.isCircuitBreakerOpen()) {
                logger.debug("实例被熔断，跳过: instanceId={}", instance.getId());
                continue;
            }

            double score = calculateComprehensiveScore(instance, metrics);
            
            logger.debug("实例评分: instanceId={}, businessId={}, score={}", 
                    instance.getId(), instance.getBusinessId(), score);

            if (score > bestScore) {
                bestScore = score;
                bestInstance = instance;
            }
        }

        if (bestInstance == null) {
            throw new BusinessException("NO_HEALTHY_INSTANCE", "所有API实例都不可用或被熔断");
        }

        logger.info("最佳实例评分: businessId={}, score={}", bestInstance.getBusinessId(), bestScore);
        return bestInstance;
    }

    /**
     * 计算综合评分
     */
    private double calculateComprehensiveScore(ApiInstanceEntity instance, InstanceMetricsEntity metrics) {
        
        // 成功率评分 (0-1)
        double successRate = calculateSuccessRateScore(metrics);
        
        // 延迟评分 (0-1, 延迟越低分数越高)
        double latencyScore = calculateLatencyScore(metrics);
        
        // 负载评分 (0-1, 负载越低分数越高)
        double loadScore = calculateLoadScore(metrics);
        
        // 优先级评分 (0-1)
        double priorityScore = calculatePriorityScore(instance);
        
        // 加权综合评分
        return successRate * SUCCESS_RATE_WEIGHT +
               latencyScore * LATENCY_WEIGHT +
               loadScore * LOAD_WEIGHT +
               priorityScore * PRIORITY_WEIGHT;
    }

    /**
     * 计算成功率评分
     */
    private double calculateSuccessRateScore(InstanceMetricsEntity metrics) {
        if (metrics == null) {
            // 冷启动实例，给予默认成功率
            return COLD_START_DEFAULT_SUCCESS_RATE;
        }
        
        return metrics.getSuccessRate();
    }

    /**
     * 计算延迟评分
     */
    private double calculateLatencyScore(InstanceMetricsEntity metrics) {
        if (metrics == null) {
            // 冷启动实例，给予中等延迟评分
            return calculateLatencyScoreFromMs(COLD_START_DEFAULT_LATENCY_MS);
        }
        
        double avgLatency = metrics.getAverageLatency();
        return calculateLatencyScoreFromMs((long) avgLatency);
    }

    /**
     * 根据延迟毫秒数计算评分
     */
    private double calculateLatencyScoreFromMs(long latencyMs) {
        if (latencyMs <= LATENCY_SCORE_BASELINE_MS) {
            return 1.0; // 满分
        }
        if (latencyMs >= LATENCY_SCORE_MAX_MS) {
            return 0.0; // 零分
        }
        
        // 线性评分
        return 1.0 - (double) (latencyMs - LATENCY_SCORE_BASELINE_MS) / 
                     (LATENCY_SCORE_MAX_MS - LATENCY_SCORE_BASELINE_MS);
    }

    /**
     * 计算负载评分
     */
    private double calculateLoadScore(InstanceMetricsEntity metrics) {
        if (metrics == null) {
            return 1.0; // 冷启动实例，无负载
        }
        
        int concurrency = metrics.getConcurrency();
        if (concurrency <= 0) {
            return 1.0; // 满分
        }
        if (concurrency >= LOAD_SCORE_MAX_CONCURRENCY) {
            return MIN_INSTANCE_WEIGHT; // 保底分数，避免饥饿
        }
        
        // 线性评分
        return Math.max(MIN_INSTANCE_WEIGHT, 
                       1.0 - (double) concurrency / LOAD_SCORE_MAX_CONCURRENCY);
    }

    /**
     * 计算优先级评分
     */
    private double calculatePriorityScore(ApiInstanceEntity instance) {
        Integer priority = instance.getPriority();
        if (priority == null || priority <= 0) {
            return 0.0;
        }
        
        // 优先级按0-100归一化
        return Math.min(1.0, priority / 100.0);
    }
} 