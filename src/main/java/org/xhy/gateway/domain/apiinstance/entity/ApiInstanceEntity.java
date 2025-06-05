package org.xhy.gateway.domain.apiinstance.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API实例领域实体
 * 对应数据库表：api_instance_registry
 * 
 * @author xhy
 * @since 1.0.0
 */
@TableName(value = "api_instance_registry", autoResultMap = true)
public class ApiInstanceEntity {

    /**
     * API 业务实例的唯一标识符 (UUID 字符串，由应用层生成)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 所属项目的 ID，外键关联 projects 表
     */
    @TableField("project_id")
    private String projectId;

    /**
     * 所属用户 ID (可选)，用于用户级别的 API 资源隔离
     */
    @TableField("user_id")
    private String userId;

    /**
     * API 的逻辑标识符，如 "gpt4o", "sms_sender"
     */
    @TableField("api_identifier")
    private String apiIdentifier;

    /**
     * API 的类型，如 "MODEL", "PAYMENT_GATEWAY", "NOTIFICATION_SERVICE"
     */
    @TableField("api_type")
    private ApiType apiType;

    /**
     * 项目方内部用于识别此 API 实例的业务 ID，由 Gateway 返回给调用方
     */
    @TableField("business_id")
    private String businessId;

    /**
     * 影响 Gateway 调度决策的实例级参数，JSONB 格式
     * 例如：{"priority": 100, "cost_per_unit": 0.0001, "initial_weight": 50}
     */
    @TableField(value = "routing_params", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> routingParams;

    /**
     * API 实例的当前状态：ACTIVE (活跃), INACTIVE (非活跃), DEPRECATED (已弃用)
     */
    @TableField("status")
    private ApiInstanceStatus status;

    /**
     * 额外扩展信息，JSONB 格式，供 Gateway 内部决策或未来扩展使用
     */
    @TableField(value = "metadata", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> metadata;

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
    public ApiInstanceEntity() {
        this.status = ApiInstanceStatus.ACTIVE;
    }


    // 领域行为方法

    /**
     * 激活API实例
     */
    public void activate() {
        this.status = ApiInstanceStatus.ACTIVE;
    }

    /**
     * 停用API实例
     */
    public void deactivate() {
        this.status = ApiInstanceStatus.INACTIVE;
    }

    /**
     * 标记为已弃用
     */
    public void deprecate() {
        this.status = ApiInstanceStatus.DEPRECATED;
    }

    /**
     * 检查API实例是否可用
     */
    public boolean isAvailable() {
        return ApiInstanceStatus.ACTIVE.equals(this.status);
    }

    /**
     * 更新路由参数
     */
    public void updateRoutingParams(Map<String, Object> routingParams) {
        this.routingParams = routingParams;
    }

    /**
     * 更新元数据
     */
    public void updateMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * 获取优先级（从路由参数中）
     */
    public Integer getPriority() {
        if (routingParams != null && routingParams.containsKey("priority")) {
            return (Integer) routingParams.get("priority");
        }
        return 0; // 默认优先级
    }

    /**
     * 获取单位成本（从路由参数中）
     */
    public Double getCostPerUnit() {
        if (routingParams != null && routingParams.containsKey("cost_per_unit")) {
            return (Double) routingParams.get("cost_per_unit");
        }
        return 0.0; // 默认成本
    }

    /**
     * 获取初始权重（从路由参数中）
     */
    public Integer getInitialWeight() {
        if (routingParams != null && routingParams.containsKey("initial_weight")) {
            return (Integer) routingParams.get("initial_weight");
        }
        return 1; // 默认权重
    }

    // Getter 和 Setter 方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApiIdentifier() {
        return apiIdentifier;
    }

    public void setApiIdentifier(String apiIdentifier) {
        this.apiIdentifier = apiIdentifier;
    }

    public ApiType getApiType() {
        return apiType;
    }

    public void setApiType(ApiType apiType) {
        this.apiType = apiType;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public Map<String, Object> getRoutingParams() {
        return routingParams;
    }

    public void setRoutingParams(Map<String, Object> routingParams) {
        this.routingParams = routingParams;
    }

    public ApiInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(ApiInstanceStatus status) {
        this.status = status;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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
        return "ApiInstanceEntity{" +
                "id='" + id + '\'' +
                ", projectId='" + projectId + '\'' +
                ", userId='" + userId + '\'' +
                ", apiIdentifier='" + apiIdentifier + '\'' +
                ", apiType=" + apiType +
                ", businessId='" + businessId + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 