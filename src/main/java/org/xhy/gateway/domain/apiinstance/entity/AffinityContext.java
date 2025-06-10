package org.xhy.gateway.domain.apiinstance.entity;

import java.time.LocalDateTime;

/**
 * 亲和性上下文
 * 封装影响实例选择亲和性的所有因素
 * 
 * @author xhy
 * @since 1.0.0
 */
public class AffinityContext {

    /**
     * 亲和性类型
     */
    private final String affinityType;

    /**
     * 亲和性标识符
     */
    private final String affinityKey;

    /**
     * 亲和性强度
     */
    private final AffinityStrength strength;

    /**
     * 亲和性过期时间（可选）
     */
    private final LocalDateTime expiresAt;

    public AffinityContext(String affinityType, String affinityKey) {
        this(affinityType, affinityKey, AffinityStrength.PREFERRED, null);
    }

    public AffinityContext(String affinityType, String affinityKey, AffinityStrength strength) {
        this(affinityType, affinityKey, strength, null);
    }

    public AffinityContext(String affinityType, String affinityKey, AffinityStrength strength, LocalDateTime expiresAt) {
        this.affinityType = affinityType;
        this.affinityKey = affinityKey;
        this.strength = strength;
        this.expiresAt = expiresAt;
    }

    /**
     * 构建亲和性绑定的唯一键
     */
    public String getBindingKey() {
        return affinityType + ":" + affinityKey;
    }

    /**
     * 检查亲和性是否有效
     */
    public boolean isValid() {
        return affinityType != null && !affinityType.trim().isEmpty()
            && affinityKey != null && !affinityKey.trim().isEmpty();
    }

    /**
     * 检查是否已过期
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public String getAffinityType() {
        return affinityType;
    }

    public String getAffinityKey() {
        return affinityKey;
    }

    public AffinityStrength getStrength() {
        return strength;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    @Override
    public String toString() {
        return "AffinityContext{" +
                "affinityType='" + affinityType + '\'' +
                ", affinityKey='" + affinityKey + '\'' +
                ", strength=" + strength +
                ", expiresAt=" + expiresAt +
                '}';
    }
} 