package org.xhy.gateway.application.dto;

/**
 * 观测概览DTO
 * 用于观测页面顶部的统计卡片展示
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ObservationOverviewDTO {

    /**
     * 总实例数
     */
    private Long totalInstances;

    /**
     * 健康实例数
     */
    private Long healthyInstances;

    /**
     * 活跃实例数
     */
    private Long activeInstances;

    /**
     * 异常实例数
     */
    private Long faultyInstances;

    /**
     * 熔断实例数
     */
    private Long circuitBreakerInstances;

    /**
     * 指定时间窗口内的总调用量
     */
    private Long totalCallCount;

    /**
     * 指定时间窗口内的平均成功率
     */
    private Double averageSuccessRate;

    /**
     * 指定时间窗口内的平均延迟
     */
    private Double averageLatency;

    public ObservationOverviewDTO() {
    }

    public ObservationOverviewDTO(Long totalInstances, Long healthyInstances, Long activeInstances, 
                                 Long faultyInstances, Long circuitBreakerInstances, Long totalCallCount, 
                                 Double averageSuccessRate, Double averageLatency) {
        this.totalInstances = totalInstances;
        this.healthyInstances = healthyInstances;
        this.activeInstances = activeInstances;
        this.faultyInstances = faultyInstances;
        this.circuitBreakerInstances = circuitBreakerInstances;
        this.totalCallCount = totalCallCount;
        this.averageSuccessRate = averageSuccessRate;
        this.averageLatency = averageLatency;
    }

    public Long getTotalInstances() {
        return totalInstances;
    }

    public void setTotalInstances(Long totalInstances) {
        this.totalInstances = totalInstances;
    }

    public Long getHealthyInstances() {
        return healthyInstances;
    }

    public void setHealthyInstances(Long healthyInstances) {
        this.healthyInstances = healthyInstances;
    }

    public Long getActiveInstances() {
        return activeInstances;
    }

    public void setActiveInstances(Long activeInstances) {
        this.activeInstances = activeInstances;
    }

    public Long getFaultyInstances() {
        return faultyInstances;
    }

    public void setFaultyInstances(Long faultyInstances) {
        this.faultyInstances = faultyInstances;
    }

    public Long getCircuitBreakerInstances() {
        return circuitBreakerInstances;
    }

    public void setCircuitBreakerInstances(Long circuitBreakerInstances) {
        this.circuitBreakerInstances = circuitBreakerInstances;
    }

    public Long getTotalCallCount() {
        return totalCallCount;
    }

    public void setTotalCallCount(Long totalCallCount) {
        this.totalCallCount = totalCallCount;
    }

    public Double getAverageSuccessRate() {
        return averageSuccessRate;
    }

    public void setAverageSuccessRate(Double averageSuccessRate) {
        this.averageSuccessRate = averageSuccessRate;
    }

    public Double getAverageLatency() {
        return averageLatency;
    }

    public void setAverageLatency(Double averageLatency) {
        this.averageLatency = averageLatency;
    }

    @Override
    public String toString() {
        return "ObservationOverviewDTO{" +
                "totalInstances=" + totalInstances +
                ", healthyInstances=" + healthyInstances +
                ", activeInstances=" + activeInstances +
                ", faultyInstances=" + faultyInstances +
                ", circuitBreakerInstances=" + circuitBreakerInstances +
                ", totalCallCount=" + totalCallCount +
                ", averageSuccessRate=" + averageSuccessRate +
                ", averageLatency=" + averageLatency +
                '}';
    }
} 