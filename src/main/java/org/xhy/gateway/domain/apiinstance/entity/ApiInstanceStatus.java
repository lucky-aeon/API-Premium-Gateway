package org.xhy.gateway.domain.apiinstance.entity;

/**
 * API实例状态枚举
 * 
 * @author xhy
 * @since 1.0.0
 */
public enum ApiInstanceStatus {
    
    /**
     * 活跃状态
     */
    ACTIVE("ACTIVE", "活跃"),
    
    /**
     * 非活跃状态
     */
    INACTIVE("INACTIVE", "非活跃"),
    
    /**
     * 已弃用状态
     */
    DEPRECATED("DEPRECATED", "已弃用");

    private final String code;
    private final String description;

    ApiInstanceStatus(String code, String description) {
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
    public static ApiInstanceStatus fromCode(String code) {
        for (ApiInstanceStatus status : ApiInstanceStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown API instance status code: " + code);
    }
} 