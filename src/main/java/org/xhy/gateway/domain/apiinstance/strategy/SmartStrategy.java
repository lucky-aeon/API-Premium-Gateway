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
 * 智能负载均衡策略
 * 通过综合评分算法直接选择最优实例
 * 考虑成功率、延迟、负载等多个指标
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class SmartStrategy implements LoadBalancingStrategy {

    private static final Logger logger = LoggerFactory.getLogger(SmartStrategy.class);

    // 评分权重配置
    private static final double SUCCESS_RATE_WEIGHT = 0.4;     // 成功率权重 40%
    private static final double LATENCY_WEIGHT = 0.4;          // 延迟权重 40%
    private static final double LOAD_WEIGHT = 0.2;             // 负载权重 20%

    // 评分标准配置
    private static final double MAX_SCORE = 100.0;             // 最高分
    private static final double MIN_SCORE = 0.0;               // 最低分
    private static final double LATENCY_SCORE_MAX_MS = 2000.0; // 延迟评分基准值
    private static final int LOAD_SCORE_MAX_CONCURRENCY = 100; // 负载评分基准值

    // 冷启动默认值
    private static final double COLD_START_SUCCESS_RATE = 1.0;
    private static final double COLD_START_LATENCY = 1000.0;
    private static final int COLD_START_CONCURRENCY = 1;

    @Override
    public String getStrategyName() {
        return "智能策略";
    }

    @Override
    public String getDescription() {
        return "通过综合评分算法选择最优实例，考虑成功率、延迟、负载等多个指标";
    }

    @Override
    public LoadBalancingType getStrategyType() {
        return LoadBalancingType.SMART;
    }

    @Override
    public ApiInstanceEntity selectInstance(List<ApiInstanceEntity> instances, 
                                          Map<String, InstanceMetricsEntity> metricsMap) {
        
        if (instances.isEmpty()) {
            throw new IllegalArgumentException("候选实例列表不能为空");
        }

        logger.debug("智能策略开始综合评分，候选实例数量: {}", instances.size());

        // 过滤掉被熔断的实例
        List<ApiInstanceEntity> availableInstances = instances.stream()
                .filter(instance -> {
                    InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
                    return metrics == null || !metrics.isCircuitBreakerOpen();
                })
                .toList();

        if (availableInstances.isEmpty()) {
            logger.warn("所有实例都被熔断，返回第一个实例");
            return instances.get(0);
        }

        // 计算每个实例的综合得分，选择得分最高的实例
        ApiInstanceEntity selected = availableInstances.stream()
                .max(Comparator.comparingDouble(instance -> calculateComprehensiveScore(instance, metricsMap)))
                .orElse(availableInstances.get(0));

        double score = calculateComprehensiveScore(selected, metricsMap);
        logger.info("智能策略选择实例: businessId={}, 综合得分={:.2f}", 
                selected.getBusinessId(), score);
        
        return selected;
    }

    /**
     * 计算实例的综合得分
     * 
     * @param instance 实例
     * @param metricsMap 指标映射
     * @return 综合得分 (0-100)
     */
    private double calculateComprehensiveScore(ApiInstanceEntity instance, 
                                             Map<String, InstanceMetricsEntity> metricsMap) {
        
        InstanceMetricsEntity metrics = metricsMap.get(instance.getId());
        
        // 获取实例指标
        double successRate = getSuccessRate(metrics);
        double latency = getAverageLatency(metrics);
        int concurrency = getConcurrency(metrics);
        
        // 计算各项得分
        double successRateScore = calculateSuccessRateScore(successRate);
        double latencyScore = calculateLatencyScore(latency);
        double loadScore = calculateLoadScore(concurrency);
        
        // 计算综合得分
        double comprehensiveScore = successRateScore * SUCCESS_RATE_WEIGHT
                                  + latencyScore * LATENCY_WEIGHT
                                  + loadScore * LOAD_WEIGHT;
        
        logger.debug("实例评分详情: businessId={}, 成功率={:.3f}({:.1f}分), 延迟={:.1f}ms({:.1f}分), 负载={}({:.1f}分), 综合得分={:.2f}", 
                instance.getBusinessId(), successRate, successRateScore, latency, latencyScore, 
                concurrency, loadScore, comprehensiveScore);
        
        return comprehensiveScore;
    }

    /**
     * 计算成功率得分
     * 成功率越高，得分越高
     */
    private double calculateSuccessRateScore(double successRate) {
        return successRate * MAX_SCORE;
    }

    /**
     * 计算延迟得分
     * 延迟越低，得分越高
     */
    private double calculateLatencyScore(double latency) {
        if (latency <= 0) {
            return MAX_SCORE;
        }
        
        // 使用反比例函数：得分 = 100 * (基准值 / (基准值 + 实际延迟))
        // 延迟为0时得分100，延迟等于基准值时得分50，延迟越高得分越低
        double score = MAX_SCORE * (LATENCY_SCORE_MAX_MS / (LATENCY_SCORE_MAX_MS + latency));
        return Math.max(MIN_SCORE, score);
    }

    /**
     * 计算负载得分
     * 并发数越低，得分越高
     */
    private double calculateLoadScore(int concurrency) {
        if (concurrency <= 0) {
            return MAX_SCORE;
        }
        
        // 使用反比例函数：得分 = 100 * (基准值 / (基准值 + 实际并发))
        double score = MAX_SCORE * ((double) LOAD_SCORE_MAX_CONCURRENCY / (LOAD_SCORE_MAX_CONCURRENCY + concurrency));
        return Math.max(MIN_SCORE, score);
    }

    /**
     * 获取实例成功率
     */
    private double getSuccessRate(InstanceMetricsEntity metrics) {
        if (metrics == null) {
            return COLD_START_SUCCESS_RATE;
        }
        return metrics.getSuccessRate();
    }

    /**
     * 获取实例平均延迟
     */
    private double getAverageLatency(InstanceMetricsEntity metrics) {
        if (metrics == null) {
            return COLD_START_LATENCY;
        }
        return metrics.getAverageLatency();
    }

    /**
     * 获取实例并发数
     */
    private int getConcurrency(InstanceMetricsEntity metrics) {
        if (metrics == null) {
            return COLD_START_CONCURRENCY;
        }
        return metrics.getConcurrency();
    }

    @Override
    public boolean isApplicable(List<ApiInstanceEntity> candidates, 
                               Map<String, InstanceMetricsEntity> metricsMap) {
        // 智能策略总是适用
        return true;
    }
} 