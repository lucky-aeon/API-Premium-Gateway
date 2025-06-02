package org.xhy.gateway.domain.project.entity;

import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

/**
 * 项目领域实体
 * 对应数据库表：projects
 * 
 * @author xhy
 * @since 1.0.0
 */
@TableName("projects")
public class ProjectEntity {

    /**
     * 项目的唯一标识符 (UUID 字符串，由应用层生成)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 项目名称，必须唯一
     */
    @TableField("name")
    private String name;

    /**
     * 项目的详细描述
     */
    @TableField("description")
    private String description;

    /**
     * 用于项目认证的 API Key，必须唯一且安全存储
     */
    @TableField("api_key")
    private String apiKey;

    /**
     * 项目状态：ACTIVE (活跃), INACTIVE (非活跃)
     */
    @TableField("status")
    private ProjectStatus status;

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
    public ProjectEntity() {
        this.status = ProjectStatus.ACTIVE;
    }

    public ProjectEntity(String name, String description, String apiKey) {
        this();
        this.name = name;
        this.description = description;
        this.apiKey = apiKey;
    }

    // 领域行为方法
    
    /**
     * 激活项目
     */
    public void activate() {
        this.status = ProjectStatus.ACTIVE;
    }

    /**
     * 停用项目
     */
    public void deactivate() {
        this.status = ProjectStatus.INACTIVE;
    }

    /**
     * 检查项目是否处于活跃状态
     */
    public boolean isActive() {
        return ProjectStatus.ACTIVE.equals(this.status);
    }

    /**
     * 更新项目信息
     */
    public void updateInfo(String name, String description) {
        this.name = name;
        this.description = description;
    }

    /**
     * 重新生成API Key
     */
    public void regenerateApiKey(String newApiKey) {
        this.apiKey = newApiKey;
    }

    // Getter 和 Setter 方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public ProjectStatus getStatus() {
        return status;
    }

    public void setStatus(ProjectStatus status) {
        this.status = status;
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
        return "ProjectEntity{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
} 