package org.xhy.gateway.domain.apiinstance.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.apiinstance.entity.LoadBalancingType;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;

import jakarta.annotation.PostConstruct;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 负载均衡策略工厂
 * 使用 Spring 自动注入和注册所有负载均衡策略
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class LoadBalancingStrategyFactory {

    private static final Logger logger = LoggerFactory.getLogger(LoadBalancingStrategyFactory.class);

    /**
     * Spring 自动注入所有 LoadBalancingStrategy 实现
     */
    @Autowired
    private List<LoadBalancingStrategy> allStrategies;

    /**
     * 策略映射表（策略类型 -> 策略实现）
     */
    private final Map<LoadBalancingType, LoadBalancingStrategy> strategyMap = new EnumMap<>(LoadBalancingType.class);

    /**
     * 初始化方法：自动注册所有策略
     */
    @PostConstruct
    public void initStrategies() {
        logger.info("开始初始化负载均衡策略...");
        
        for (LoadBalancingStrategy strategy : allStrategies) {
            LoadBalancingType type = strategy.getStrategyType();
            strategyMap.put(type, strategy);
            
            logger.info("注册负载均衡策略: {} -> {}", 
                    type.getCode(), strategy.getClass().getSimpleName());
        }
        
        logger.info("负载均衡策略初始化完成，共注册 {} 个策略", strategyMap.size());
        
        // 验证所有策略类型都有对应的实现
        for (LoadBalancingType type : LoadBalancingType.values()) {
            if (!strategyMap.containsKey(type)) {
                logger.warn("策略类型 {} 没有对应的实现", type.getCode());
            }
        }
    }

    /**
     * 根据负载均衡类型获取策略
     * 
     * @param loadBalancingType 负载均衡类型
     * @return 对应的负载均衡策略
     * @throws IllegalArgumentException 如果找不到对应的策略实现
     */
    public LoadBalancingStrategy getStrategy(LoadBalancingType loadBalancingType) {
        LoadBalancingStrategy strategy = strategyMap.get(loadBalancingType);
        
        if (strategy == null) {
            throw new IllegalArgumentException(
                    String.format("未找到负载均衡策略类型 %s 的实现", loadBalancingType.getCode()));
        }
        
        return strategy;
    }

    /**
     * 获取默认策略（智能策略）
     * 
     * @return 默认的负载均衡策略
     */
    public LoadBalancingStrategy getDefaultStrategy() {
        return getStrategy(LoadBalancingType.SMART);
    }

    /**
     * 获取所有已注册的策略
     * 
     * @return 策略映射表的副本
     */
    public Map<LoadBalancingType, LoadBalancingStrategy> getAllStrategies() {
        return Map.copyOf(strategyMap);
    }

    /**
     * 检查策略类型是否已注册
     * 
     * @param loadBalancingType 负载均衡类型
     * @return 是否已注册
     */
    public boolean isStrategyRegistered(LoadBalancingType loadBalancingType) {
        return strategyMap.containsKey(loadBalancingType);
    }

    /**
     * 获取已注册策略的数量
     * 
     * @return 策略数量
     */
    public int getRegisteredStrategyCount() {
        return strategyMap.size();
    }

    /**
     * 智能选择策略
     * 根据候选实例和指标数据智能选择最合适的策略
     */
    public LoadBalancingStrategy getSmartStrategy(List<ApiInstanceEntity> candidates,
                                                 Map<String, InstanceMetricsEntity> metricsMap) {
        
        // 1. 如果没有足够的指标数据，使用轮询策略
        if (hasInsufficientMetrics(candidates, metricsMap)) {
            return getStrategy(LoadBalancingType.ROUND_ROBIN);
        }

        // 2. 如果延迟差异明显，使用延迟优先策略
        if (hasSignificantLatencyDifference(candidates, metricsMap)) {
            return getStrategy(LoadBalancingType.LATENCY_FIRST);
        }

        // 3. 如果成功率差异明显，使用成功率优先策略
        if (hasSignificantSuccessRateDifference(candidates, metricsMap)) {
            return getStrategy(LoadBalancingType.SUCCESS_RATE_FIRST);
        }

        // 4. 默认使用轮询策略
        return getStrategy(LoadBalancingType.ROUND_ROBIN);
    }

    /**
     * 检查是否有足够的指标数据
     */
    private boolean hasInsufficientMetrics(List<ApiInstanceEntity> candidates,
                                          Map<String, InstanceMetricsEntity> metricsMap) {
        long instancesWithMetrics = candidates.stream()
                .mapToLong(instance -> {
                    InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
                    return (metrics != null && (metrics.getSuccessCount() + metrics.getFailureCount()) >= 10) ? 1 : 0;
                })
                .sum();
        
        return instancesWithMetrics < candidates.size() * 0.7; // 至少70%的实例有足够数据
    }

    /**
     * 检查是否有明显的延迟差异
     */
    private boolean hasSignificantLatencyDifference(List<ApiInstanceEntity> candidates,
                                                   Map<String, InstanceMetricsEntity> metricsMap) {
        double maxLatency = 0;
        double minLatency = Double.MAX_VALUE;
        
        for (ApiInstanceEntity instance : candidates) {
            InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
            if (metrics != null) {
                double latency = metrics.getAverageLatency();
                maxLatency = Math.max(maxLatency, latency);
                minLatency = Math.min(minLatency, latency);
            }
        }
        
        // 如果最大延迟比最小延迟高50%以上，认为有明显差异
        return maxLatency > minLatency * 1.5;
    }

    /**
     * 检查是否有明显的成功率差异
     */
    private boolean hasSignificantSuccessRateDifference(List<ApiInstanceEntity> candidates,
                                                       Map<String, InstanceMetricsEntity> metricsMap) {
        double maxSuccessRate = 0;
        double minSuccessRate = 1.0;
        
        for (ApiInstanceEntity instance : candidates) {
            InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
            if (metrics != null) {
                double successRate = metrics.getSuccessRate();
                maxSuccessRate = Math.max(maxSuccessRate, successRate);
                minSuccessRate = Math.min(minSuccessRate, successRate);
            }
        }
        
        // 如果成功率差异超过10%，认为有明显差异
        return maxSuccessRate - minSuccessRate > 0.1;
    }
} 