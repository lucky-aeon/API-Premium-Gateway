package org.xhy.gateway.domain.apiinstance.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.LoadBalancingType;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 轮询负载均衡策略
 * 简单轮询选择实例，适用于实例性能相近的场景
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class RoundRobinStrategy implements LoadBalancingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(RoundRobinStrategy.class);
    
    private final AtomicLong counter = new AtomicLong(0);

    @Override
    public String getStrategyName() {
        return "ROUND_ROBIN";
    }

    @Override
    public String getDescription() {
        return "轮询策略：依次选择每个可用实例，适用于实例性能相近的场景";
    }

    @Override
    public LoadBalancingType getStrategyType() {
        return LoadBalancingType.ROUND_ROBIN;
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

        // 轮询选择
        long index = counter.getAndIncrement() % availableInstances.size();
        ApiInstanceEntity selected = availableInstances.get((int) index);
        
        logger.debug("轮询策略选择实例: businessId={}, 当前计数={}", 
                selected.getBusinessId(), counter.get());
        
        return selected;
    }

    @Override
    public boolean isApplicable(List<ApiInstanceEntity> candidates, 
                               Map<String, InstanceMetricsEntity> metricsMap) {
        // 轮询策略总是适用
        return true;
    }
} 