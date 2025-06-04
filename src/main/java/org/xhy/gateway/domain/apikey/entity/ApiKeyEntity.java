package org.xhy.gateway.domain.apikey.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * API Key 领域实体
 * 对应数据库表：api_keys
 * 
 * @author xhy
 * @since 1.0.0
 */
@TableName("api_keys")
public class ApiKeyEntity {

    /**
     * API Key 记录的唯一标识符 (UUID 字符串，由应用层生成)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 实际的 API Key 字符串，必须全局唯一且安全存储
     */
    @TableField("api_key_value")
    private String apiKeyValue;

    /**
     * 对该 API Key 的描述
     */
    @TableField("description")
    private String description;

    /**
     * API Key 的状态：ACTIVE (激活), REVOKED (已撤销), EXPIRED (已过期), UNUSED (未使用)
     */
    @TableField("status")
    private ApiKeyStatus status;

    /**
     * API Key 的颁发时间
     */
    @TableField(value = "issued_at", fill = FieldFill.INSERT)
    private LocalDateTime issuedAt;

    /**
     * API Key 的过期时间 (可选)。如果为 NULL，则永不过期
     */
    @TableField("expires_at")
    private LocalDateTime expiresAt;

    /**
     * API Key 最后一次被使用的时间，可用于审计和清理过期/不活跃 Key
     */
    @TableField("last_used_at")
    private LocalDateTime lastUsedAt;

    /**
     * 记录创建时间
     */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 记录最后更新时间，每次更新时自动修改
     */
    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 构造函数
    public ApiKeyEntity() {
        this.status = ApiKeyStatus.UNUSED;
    }

    public ApiKeyEntity(String apiKeyValue, String description) {
        this();
        this.apiKeyValue = apiKeyValue;
        this.description = description;
    }

    public ApiKeyEntity(String apiKeyValue, String description, LocalDateTime expiresAt) {
        this(apiKeyValue, description);
        this.expiresAt = expiresAt;
    }

    // 领域行为方法

    /**
     * 激活 API Key
     */
    public void activate() {
        if (this.status == ApiKeyStatus.REVOKED) {
            throw new IllegalStateException("已撤销的 API Key 无法激活");
        }
        this.status = ApiKeyStatus.ACTIVE;
    }

    /**
     * 撤销 API Key
     */
    public void revoke() {
        this.status = ApiKeyStatus.REVOKED;
    }

    /**
     * 标记 API Key 为已过期
     */
    public void markExpired() {
        this.status = ApiKeyStatus.EXPIRED;
    }

    /**
     * 检查 API Key 是否可用
     */
    public boolean isUsable() {
        // 检查状态
        if (!status.isUsable()) {
            return false;
        }
        
        // 检查是否过期
        if (expiresAt != null && LocalDateTime.now().isAfter(expiresAt)) {
            // 如果过期了，自动标记为过期状态
            markExpired();
            return false;
        }
        
        return true;
    }

    /**
     * 检查 API Key 是否已过期
     */
    public boolean isExpired() {
        if (expiresAt == null) {
            return false; // 永不过期
        }
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * 更新最后使用时间
     */
    public void updateLastUsedTime() {
        this.lastUsedAt = LocalDateTime.now();
        
        // 如果是第一次使用，将状态从 UNUSED 改为 ACTIVE
        if (this.status == ApiKeyStatus.UNUSED) {
            this.status = ApiKeyStatus.ACTIVE;
        }
    }

    /**
     * 更新描述
     */
    public void updateDescription(String description) {
        this.description = description;
    }

    /**
     * 设置过期时间
     */
    public void setExpirationTime(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    /**
     * 获取剩余有效天数
     */
    public Long getRemainingDays() {
        if (expiresAt == null) {
            return null; // 永不过期
        }
        
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(expiresAt)) {
            return 0L; // 已过期
        }
        
        return java.time.Duration.between(now, expiresAt).toDays();
    }

    // Getter 和 Setter 方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApiKeyValue() {
        return apiKeyValue;
    }

    public void setApiKeyValue(String apiKeyValue) {
        this.apiKeyValue = apiKeyValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ApiKeyStatus getStatus() {
        return status;
    }

    public void setStatus(ApiKeyStatus status) {
        this.status = status;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(LocalDateTime lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "ApiKeyEntity{" +
                "id='" + id + '\'' +
                ", apiKeyValue='***MASKED***'" +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", issuedAt=" + issuedAt +
                ", expiresAt=" + expiresAt +
                ", lastUsedAt=" + lastUsedAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 