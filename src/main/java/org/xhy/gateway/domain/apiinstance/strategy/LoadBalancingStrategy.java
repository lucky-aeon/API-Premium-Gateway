package org.xhy.gateway.domain.apiinstance.strategy;

import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.LoadBalancingType;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;

import java.util.List;
import java.util.Map;

/**
 * 负载均衡策略接口
 * 定义不同的负载均衡算法
 * 
 * @author xhy
 * @since 1.0.0
 */
public interface LoadBalancingStrategy {

    /**
     * 策略名称
     */
    String getStrategyName();

    /**
     * 策略描述
     */
    String getDescription();

    /**
     * 获取策略类型（用于自动注册）
     */
    LoadBalancingType getStrategyType();

    /**
     * 选择最佳实例
     * 
     * @param candidates 候选实例列表
     * @param metricsMap 实例指标映射
     * @return 选中的实例
     */
    ApiInstanceEntity selectInstance(List<ApiInstanceEntity> candidates, 
                                   Map<String, InstanceMetricsEntity> metricsMap);

    /**
     * 策略是否适用于当前场景
     * 
     * @param candidates 候选实例列表
     * @param metricsMap 实例指标映射
     * @return 是否适用
     */
    default boolean isApplicable(List<ApiInstanceEntity> candidates, 
                                Map<String, InstanceMetricsEntity> metricsMap) {
        return true;
    }
} 