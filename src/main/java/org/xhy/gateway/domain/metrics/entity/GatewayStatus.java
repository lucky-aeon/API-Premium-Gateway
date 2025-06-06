package org.xhy.gateway.domain.metrics.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * Gateway 判断的 API 实例状态枚举
 * 
 * @author xhy
 * @since 1.0.0
 */
public enum GatewayStatus {
    
    /**
     * 健康状态 - 实例工作正常
     */
    HEALTHY("HEALTHY", "健康"),
    
    /**
     * 降级状态 - 实例性能下降但仍可用
     */
    DEGRADED("DEGRADED", "降级"),
    
    /**
     * 故障状态 - 实例出现问题但未被熔断
     */
    FAULTY("FAULTY", "故障"),
    
    /**
     * 熔断器打开状态 - 实例被熔断，暂停路由
     */
    CIRCUIT_BREAKER_OPEN("CIRCUIT_BREAKER_OPEN", "熔断器打开");

    @EnumValue
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
     * 根据代码获取枚举
     */
    public static GatewayStatus fromCode(String code) {
        for (GatewayStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的网关状态代码: " + code);
    }

    /**
     * 判断是否可用于路由
     */
    public boolean isAvailableForRouting() {
        return this == HEALTHY || this == DEGRADED;
    }
} 