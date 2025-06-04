package org.xhy.gateway.domain.apikey.entity;

/**
 * API Key 状态枚举
 * 
 * @author xhy
 * @since 1.0.0
 */
public enum ApiKeyStatus {
    
    /**
     * 激活状态
     */
    ACTIVE("ACTIVE", "激活"),
    
    /**
     * 已撤销状态
     */
    REVOKED("REVOKED", "已撤销"),
    
    /**
     * 已过期状态
     */
    EXPIRED("EXPIRED", "已过期"),
    
    /**
     * 未使用状态
     */
    UNUSED("UNUSED", "未使用");

    private final String code;
    private final String description;

    ApiKeyStatus(String code, String description) {
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
    public static ApiKeyStatus fromCode(String code) {
        for (ApiKeyStatus status : ApiKeyStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown API key status code: " + code);
    }

    /**
     * 检查是否为可用状态
     */
    public boolean isUsable() {
        return this == ACTIVE || this == UNUSED;
    }

    /**
     * 检查是否为已撤销状态
     */
    public boolean isRevoked() {
        return this == REVOKED;
    }

    /**
     * 检查是否为已过期状态
     */
    public boolean isExpired() {
        return this == EXPIRED;
    }
} 