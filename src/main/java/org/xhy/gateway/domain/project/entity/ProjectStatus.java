package org.xhy.gateway.domain.project.entity;

/**
 * 项目状态枚举
 * 
 * @author xhy
 * @since 1.0.0
 */
public enum ProjectStatus {
    
    /**
     * 活跃状态
     */
    ACTIVE("ACTIVE", "活跃"),
    
    /**
     * 非活跃状态
     */
    INACTIVE("INACTIVE", "非活跃");

    private final String code;
    private final String description;

    ProjectStatus(String code, String description) {
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
    public static ProjectStatus fromCode(String code) {
        for (ProjectStatus status : ProjectStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown project status code: " + code);
    }
} 