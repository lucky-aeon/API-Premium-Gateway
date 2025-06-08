package org.xhy.gateway.domain.apiinstance.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.LoadBalancingType;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;

import java.util.List;
import java.util.Map;
import java.util.Comparator;

/**
 * 成功率优先负载均衡策略
 * 优先选择成功率最高的实例，适用于对稳定性要求高的场景
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class SuccessRateFirstStrategy implements LoadBalancingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SuccessRateFirstStrategy.class);
    
    private static final double COLD_START_DEFAULT_SUCCESS_RATE = 1.0;
    private static final long MIN_REQUESTS_FOR_EVALUATION = 10;

    @Override
    public String getStrategyName() {
        return "SUCCESS_RATE_FIRST";
    }

    @Override
    public String getDescription() {
        return "成功率优先策略：选择历史成功率最高的实例，适用于对稳定性要求高的场景";
    }

    @Override
    public LoadBalancingType getStrategyType() {
        return LoadBalancingType.SUCCESS_RATE_FIRST;
    }

    @Override
    public ApiInstanceEntity selectInstance(List<ApiInstanceEntity> candidates, 
                                          Map<String, InstanceMetricsEntity> metricsMap) {
        
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("候选实例列表不能为空");
        }

        // 过滤掉被熔断的实例
        List<ApiInstanceEntity> availableInstances = candidates.stream()
                .filter(instance -> {
                    InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
                    return metrics == null || !metrics.isCircuitBreakerOpen();
                })
                .toList();

        if (availableInstances.isEmpty()) {
            logger.warn("所有实例都被熔断，返回第一个实例");
            return candidates.get(0);
        }

        // 按成功率排序，选择成功率最高的实例
        ApiInstanceEntity selected = availableInstances.stream()
                .max(Comparator.comparingDouble(instance -> getSuccessRate(instance, metricsMap)))
                .orElse(availableInstances.get(0));

        double successRate = getSuccessRate(selected, metricsMap);
        logger.debug("成功率优先策略选择实例: businessId={}, successRate={:.3f}", 
                selected.getBusinessId(), successRate);
        
        return selected;
    }

    @Override
    public boolean isApplicable(List<ApiInstanceEntity> candidates, 
                               Map<String, InstanceMetricsEntity> metricsMap) {
        // 检查是否有足够的指标数据来判断成功率
        long instancesWithMetrics = candidates.stream()
                .mapToLong(instance -> metricsMap.containsKey(instance.getId()) ? 1 : 0)
                .sum();
        
        return instancesWithMetrics > 0;
    }

    /**
     * 获取实例的成功率
     */
    private double getSuccessRate(ApiInstanceEntity instance, Map<String, InstanceMetricsEntity> metricsMap) {
        InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
        if (metrics == null) {
            // 冷启动实例，给予默认成功率
            return COLD_START_DEFAULT_SUCCESS_RATE;
        }
        return metrics.getSuccessRate();
    }

    /**
     * 获取实例总请求数
     */
    private long getTotalRequests(ApiInstanceEntity instance, Map<String, InstanceMetricsEntity> metricsMap) {
        InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
        if (metrics == null) {
            return 0;
        }
        return metrics.getSuccessCount() + metrics.getFailureCount();
    }
} 