package org.xhy.gateway.domain.apiinstance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xhy.gateway.domain.apiinstance.entity.AffinityContext;
import org.xhy.gateway.domain.apiinstance.entity.AffinityStrength;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;
import org.xhy.gateway.domain.apiinstance.strategy.LoadBalancingStrategy;

import java.util.List;
import java.util.Map;

/**
 * 亲和性感知的策略装饰器
 * 将亲和性逻辑与负载均衡策略结合
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class AffinityAwareStrategyDecorator {

    private static final Logger logger = LoggerFactory.getLogger(AffinityAwareStrategyDecorator.class);

    private final AffinityService affinityService;

    public AffinityAwareStrategyDecorator(AffinityService affinityService) {
        this.affinityService = affinityService;
    }

    /**
     * 带亲和性的实例选择
     * 
     * @param candidates 候选实例列表
     * @param metricsMap 实例指标映射
     * @param strategy 负载均衡策略
     * @param affinityContext 亲和性上下文
     * @return 选中的实例
     */
    public ApiInstanceEntity selectInstanceWithAffinity(
            List<ApiInstanceEntity> candidates,
            Map<String, InstanceMetricsEntity> metricsMap,
            LoadBalancingStrategy strategy,
            AffinityContext affinityContext) {

        // 1. 如果没有亲和性要求，直接使用负载均衡策略
        if (affinityContext == null || !affinityContext.isValid()) {
            logger.debug("无亲和性要求，直接使用负载均衡策略: {}", strategy.getClass().getSimpleName());
            return strategy.selectInstance(candidates, metricsMap);
        }

        // 2. 检查是否有现有的亲和性绑定
        String boundInstanceId = affinityService.getBoundInstance(
            affinityContext.getAffinityType(), 
            affinityContext.getAffinityKey()
        );

        if (boundInstanceId != null) {
            // 3. 查找绑定的实例是否在候选列表中且健康
            ApiInstanceEntity boundInstance = findInstanceById(candidates, boundInstanceId);
            
            if (boundInstance != null) {
                // 绑定的实例可用，刷新绑定并返回
                affinityService.refreshBinding(
                    affinityContext.getAffinityType(),
                    affinityContext.getAffinityKey(),
                    boundInstanceId
                );
                
                logger.debug("使用亲和性绑定实例: {} -> {}", 
                    affinityContext.getBindingKey(), boundInstanceId);
                return boundInstance;
            } else {
                // 绑定的实例不可用
                logger.warn("亲和性绑定的实例不可用: {} -> {}", 
                    affinityContext.getBindingKey(), boundInstanceId);
                
                return handleUnavailableBinding(candidates, metricsMap, strategy, affinityContext);
            }
        }

        // 4. 没有现有绑定，使用负载均衡策略选择新实例并创建绑定
        ApiInstanceEntity selectedInstance = strategy.selectInstance(candidates, metricsMap);
        
        if (selectedInstance != null) {
            affinityService.createBinding(
                affinityContext.getAffinityType(),
                affinityContext.getAffinityKey(),
                selectedInstance.getId()
            );
            
            logger.info("创建新的亲和性绑定: {} -> {}", 
                affinityContext.getBindingKey(), selectedInstance.getId());
        }

        return selectedInstance;
    }

    /**
     * 处理绑定实例不可用的情况
     */
    private ApiInstanceEntity handleUnavailableBinding(
            List<ApiInstanceEntity> candidates,
            Map<String, InstanceMetricsEntity> metricsMap,
            LoadBalancingStrategy strategy,
            AffinityContext affinityContext) {

        AffinityStrength strength = affinityContext.getStrength();

        switch (strength) {
            case STRICT:
                // 严格模式：绑定实例不可用时抛出异常
                String errorMsg = String.format("严格亲和性模式下，绑定实例不可用: %s", 
                    affinityContext.getBindingKey());
                logger.error(errorMsg);
                throw new RuntimeException(errorMsg);

            case PREFERRED:
                // 优先模式：清除旧绑定，选择新实例并创建新绑定
                logger.info("优先亲和性模式下，清除不可用绑定并重新选择: {}", 
                    affinityContext.getBindingKey());
                
                affinityService.clearBinding(
                    affinityContext.getAffinityType(),
                    affinityContext.getAffinityKey()
                );

                ApiInstanceEntity newInstance = strategy.selectInstance(candidates, metricsMap);
                if (newInstance != null) {
                    affinityService.createBinding(
                        affinityContext.getAffinityType(),
                        affinityContext.getAffinityKey(),
                        newInstance.getId()
                    );
                    
                    logger.info("重新创建亲和性绑定: {} -> {}", 
                        affinityContext.getBindingKey(), newInstance.getId());
                }
                return newInstance;

            case NONE:
            default:
                // 无亲和性：直接使用负载均衡策略
                logger.debug("无亲和性模式，直接使用负载均衡策略");
                return strategy.selectInstance(candidates, metricsMap);
        }
    }

    /**
     * 根据实例ID查找实例
     */
    private ApiInstanceEntity findInstanceById(List<ApiInstanceEntity> candidates, String instanceId) {
        return candidates.stream()
                .filter(instance -> instance.getId().equals(instanceId))
                .findFirst()
                .orElse(null);
    }
} 