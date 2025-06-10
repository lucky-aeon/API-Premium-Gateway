package org.xhy.gateway.domain.apiinstance.entity;

import java.time.LocalDateTime;

/**
 * 亲和性绑定对象
 * 存储亲和性绑定关系的详细信息
 * 
 * @author xhy
 * @since 1.0.0
 */
public class AffinityBinding {

    /**
     * 绑定的实例ID
     */
    private final String instanceId;

    /**
     * 创建时间
     */
    private final LocalDateTime createTime;

    /**
     * 过期时间
     */
    private final LocalDateTime expireTime;

    /**
     * 使用次数统计
     */
    private final int useCount;

    /**
     * 最后使用时间
     */
    private final LocalDateTime lastUsedTime;

    public AffinityBinding(String instanceId) {
        this(instanceId, LocalDateTime.now(), LocalDateTime.now().plusMinutes(30), 1, LocalDateTime.now());
    }

    public AffinityBinding(String instanceId, LocalDateTime createTime, LocalDateTime expireTime, 
                          int useCount, LocalDateTime lastUsedTime) {
        this.instanceId = instanceId;
        this.createTime = createTime;
        this.expireTime = expireTime;
        this.useCount = useCount;
        this.lastUsedTime = lastUsedTime;
    }

    /**
     * 创建新的绑定对象，增加使用次数并更新过期时间
     */
    public AffinityBinding withNewExpireTime(LocalDateTime newExpireTime) {
        return new AffinityBinding(
            this.instanceId,
            this.createTime,
            newExpireTime,
            this.useCount + 1,
            LocalDateTime.now()
        );
    }

    /**
     * 检查绑定是否已过期
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireTime);
    }

    /**
     * 获取绑定存活时间（分钟）
     */
    public long getAliveDurationMinutes() {
        return java.time.Duration.between(createTime, LocalDateTime.now()).toMinutes();
    }

    public String getInstanceId() {
        return instanceId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public int getUseCount() {
        return useCount;
    }

    public LocalDateTime getLastUsedTime() {
        return lastUsedTime;
    }

    @Override
    public String toString() {
        return "AffinityBinding{" +
                "instanceId='" + instanceId + '\'' +
                ", createTime=" + createTime +
                ", expireTime=" + expireTime +
                ", useCount=" + useCount +
                ", lastUsedTime=" + lastUsedTime +
                '}';
    }
} 