package org.xhy.gateway.domain.metrics.entity;

/**
 * Gateway状态枚举
 * 
 * @author xhy
 * @since 1.0.0
 */
public enum GatewayStatus {
    
    /**
     * 健康状态
     */
    HEALTHY("HEALTHY", "健康"),
    
    /**
     * 降级状态
     */
    DEGRADED("DEGRADED", "降级"),
    
    /**
     * 故障状态
     */
    FAULTY("FAULTY", "故障"),
    
    /**
     * 熔断器开启状态
     */
    CIRCUIT_BREAKER_OPEN("CIRCUIT_BREAKER_OPEN", "熔断器开启");

    private final String code;
    private final String description;

    GatewayStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举值
     */
    public static GatewayStatus fromCode(String code) {
        for (GatewayStatus status : GatewayStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown gateway status code: " + code);
    }

    /**
     * 检查是否为健康状态
     */
    public boolean isHealthy() {
        return this == HEALTHY;
    }

    /**
     * 检查是否为降级状态
     */
    public boolean isDegraded() {
        return this == DEGRADED;
    }

    /**
     * 检查是否为故障状态
     */
    public boolean isFaulty() {
        return this == FAULTY;
    }

    /**
     * 检查熔断器是否开启
     */
    public boolean isCircuitBreakerOpen() {
        return this == CIRCUIT_BREAKER_OPEN;
    }
} 