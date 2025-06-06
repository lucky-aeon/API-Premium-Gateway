package org.xhy.gateway.domain.apiinstance.service;

/**
 * API实例选择算法配置常量
 * 
 * @author xhy
 * @since 1.0.0
 */
public final class SelectionConstants {
    
    // ======== 综合评分权重配置 ========
    
    /**
     * 成功率权重 (40%)
     */
    public static final double SUCCESS_RATE_WEIGHT = 0.4;
    
    /**
     * 延迟权重 (30%)
     */
    public static final double LATENCY_WEIGHT = 0.3;
    
    /**
     * 负载权重 (20%)
     */
    public static final double LOAD_WEIGHT = 0.2;
    
    /**
     * 优先级权重 (10%)
     */
    public static final double PRIORITY_WEIGHT = 0.1;
    
    // ======== 熔断器配置 ========
    
    /**
     * 熔断错误率阈值 (50%)
     */
    public static final double CIRCUIT_BREAKER_ERROR_RATE_THRESHOLD = 0.5;
    
    /**
     * 熔断最小请求数阈值
     */
    public static final long CIRCUIT_BREAKER_MIN_REQUEST_COUNT = 10;
    
    /**
     * 熔断冷却时间 (秒)
     */
    public static final long CIRCUIT_BREAKER_COOLDOWN_SECONDS = 30;
    
    // ======== 时间窗口配置 ========
    
    /**
     * 当前时间窗口 (分钟) - 用于实时决策
     */
    public static final int CURRENT_WINDOW_MINUTES = 1;
    
    /**
     * 短期窗口 (分钟) - 用于平滑指标
     */
    public static final int SHORT_TERM_WINDOW_MINUTES = 5;
    
    /**
     * 长期窗口 (分钟) - 用于趋势分析
     */
    public static final int LONG_TERM_WINDOW_MINUTES = 15;
    
    // ======== 冷启动配置 ========
    
    /**
     * 冷启动实例最小调用次数阈值
     */
    public static final long COLD_START_MIN_CALLS = 5;
    
    /**
     * 冷启动实例探索性流量百分比 (5%)
     */
    public static final double COLD_START_EXPLORATION_RATIO = 0.05;
    
    /**
     * 冷启动默认成功率
     */
    public static final double COLD_START_DEFAULT_SUCCESS_RATE = 1.0;
    
    /**
     * 冷启动默认延迟 (毫秒)
     */
    public static final long COLD_START_DEFAULT_LATENCY_MS = 1000;
    
    // ======== 延迟评分配置 ========
    
    /**
     * 延迟评分基准值 (毫秒) - 低于此值得满分
     */
    public static final long LATENCY_SCORE_BASELINE_MS = 500;
    
    /**
     * 延迟评分最大容忍值 (毫秒) - 超过此值得0分
     */
    public static final long LATENCY_SCORE_MAX_MS = 5000;
    
    // ======== 负载评分配置 ========
    
    /**
     * 负载评分最大并发数 - 超过此值得0分
     */
    public static final int LOAD_SCORE_MAX_CONCURRENCY = 100;
    
    /**
     * 实例最低权重保证 - 避免饥饿
     */
    public static final double MIN_INSTANCE_WEIGHT = 0.01;
    
    private SelectionConstants() {
        // 工具类不允许实例化
    }
} 