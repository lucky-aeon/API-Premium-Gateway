package org.xhy.gateway.domain.apiinstance.entity;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 负载均衡策略类型
 * 
 * @author xhy
 * @since 1.0.0
 */
public enum LoadBalancingType {
    /**
     * 智能策略 - 根据实例指标自动选择最优策略
     */
    SMART("smart", "智能策略"),
    
    /**
     * 轮询策略
     */
    ROUND_ROBIN("round_robin", "轮询"),
    
    /**
     * 成功率优先策略
     */
    SUCCESS_RATE_FIRST("success_rate_first", "成功率优先"),
    
    /**
     * 延迟优先策略
     */
    LATENCY_FIRST("latency_first", "延迟优先"),

    /**
     * 会话亲和性策略 - 同一会话路由到相同实例
     */
    SESSION_AFFINITY("session_affinity", "会话亲和性");

    private final String code;
    private final String description;

    LoadBalancingType(String code, String description) {
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
     * 根据代码获取类型
     */
    public static LoadBalancingType fromCode(String code) {
        for (LoadBalancingType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的负载均衡策略类型: " + code);
    }
} 