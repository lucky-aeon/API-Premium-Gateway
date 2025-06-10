package org.xhy.gateway.domain.apiinstance.entity;

/**
 * 亲和性强度枚举
 * 定义亲和性绑定的强度级别
 * 
 * @author xhy
 * @since 1.0.0
 */
public enum AffinityStrength {
    
    /**
     * 严格绑定
     * 绑定实例不可用时抛出异常，拒绝切换到其他实例
     */
    STRICT("strict", "严格绑定"),
    
    /**
     * 优先绑定
     * 绑定实例不可用时可以切换到其他实例
     */
    PREFERRED("preferred", "优先绑定"),
    
    /**
     * 无亲和性要求
     * 每次都重新进行负载均衡
     */
    NONE("none", "无亲和性");

    private final String code;
    private final String description;

    AffinityStrength(String code, String description) {
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
     * 根据代码获取亲和性强度
     */
    public static AffinityStrength fromCode(String code) {
        for (AffinityStrength strength : values()) {
            if (strength.code.equals(code)) {
                return strength;
            }
        }
        return PREFERRED; // 默认返回优先绑定
    }
} 