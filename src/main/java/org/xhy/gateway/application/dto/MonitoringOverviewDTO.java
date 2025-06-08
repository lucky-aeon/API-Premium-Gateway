package org.xhy.gateway.application.dto;

/**
 * 监控概览DTO
 * 用于监控页面展示核心指标统计
 * 
 * @author xhy
 * @since 1.0.0
 */
public class MonitoringOverviewDTO {

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
     * 平均成功率（百分比）
     */
    private Double averageSuccessRate;

    /**
     * 平均延迟（毫秒）
     */
    private Double averageLatency;

    /**
     * 总调用次数（最近时间窗口）
     */
    private Long totalCalls;

    public MonitoringOverviewDTO() {
    }

    public MonitoringOverviewDTO(Long totalInstances, Long healthyInstances, Long activeInstances, 
                                Double averageSuccessRate, Double averageLatency, Long totalCalls) {
        this.totalInstances = totalInstances;
        this.healthyInstances = healthyInstances;
        this.activeInstances = activeInstances;
        this.averageSuccessRate = averageSuccessRate;
        this.averageLatency = averageLatency;
        this.totalCalls = totalCalls;
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

    public Long getTotalCalls() {
        return totalCalls;
    }

    public void setTotalCalls(Long totalCalls) {
        this.totalCalls = totalCalls;
    }

    @Override
    public String toString() {
        return "MonitoringOverviewDTO{" +
                "totalInstances=" + totalInstances +
                ", healthyInstances=" + healthyInstances +
                ", activeInstances=" + activeInstances +
                ", averageSuccessRate=" + averageSuccessRate +
                ", averageLatency=" + averageLatency +
                ", totalCalls=" + totalCalls +
                '}';
    }
} 