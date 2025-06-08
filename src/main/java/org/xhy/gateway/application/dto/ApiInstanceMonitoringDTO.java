package org.xhy.gateway.application.dto;

import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;

import java.time.LocalDateTime;

/**
 * API实例监控DTO
 * 用于监控页面展示实例的详细监控信息
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ApiInstanceMonitoringDTO {

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 项目ID
     */
    private String projectId;

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * API标识符
     */
    private String apiIdentifier;

    /**
     * API类型
     */
    private ApiType apiType;

    /**
     * 实例状态
     */
    private ApiInstanceStatus status;

    /**
     * 网关状态
     */
    private GatewayStatus gatewayStatus;

    /**
     * 成功率（百分比）
     */
    private Double successRate;

    /**
     * 失败率（百分比）
     */
    private Double failureRate;

    /**
     * 平均延迟（毫秒）
     */
    private Double averageLatency;

    /**
     * 当前并发数
     */
    private Integer concurrency;

    /**
     * 最近调用次数（时间窗口内）
     */
    private Long recentCalls;

    /**
     * 成功调用次数
     */
    private Long successCount;

    /**
     * 失败调用次数
     */
    private Long failureCount;

    /**
     * 最后上报时间
     */
    private LocalDateTime lastReportedAt;

    /**
     * 优先级
     */
    private Integer priority;

    /**
     * 单位成本
     */
    private Double costPerUnit;

    /**
     * 权重
     */
    private Integer weight;

    public ApiInstanceMonitoringDTO() {
    }

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
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

    public ApiInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(ApiInstanceStatus status) {
        this.status = status;
    }

    public GatewayStatus getGatewayStatus() {
        return gatewayStatus;
    }

    public void setGatewayStatus(GatewayStatus gatewayStatus) {
        this.gatewayStatus = gatewayStatus;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public Double getFailureRate() {
        return failureRate;
    }

    public void setFailureRate(Double failureRate) {
        this.failureRate = failureRate;
    }

    public Double getAverageLatency() {
        return averageLatency;
    }

    public void setAverageLatency(Double averageLatency) {
        this.averageLatency = averageLatency;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }

    public Long getRecentCalls() {
        return recentCalls;
    }

    public void setRecentCalls(Long recentCalls) {
        this.recentCalls = recentCalls;
    }

    public Long getSuccessCount() {
        return successCount;
    }

    public void setSuccessCount(Long successCount) {
        this.successCount = successCount;
    }

    public Long getFailureCount() {
        return failureCount;
    }

    public void setFailureCount(Long failureCount) {
        this.failureCount = failureCount;
    }

    public LocalDateTime getLastReportedAt() {
        return lastReportedAt;
    }

    public void setLastReportedAt(LocalDateTime lastReportedAt) {
        this.lastReportedAt = lastReportedAt;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Double getCostPerUnit() {
        return costPerUnit;
    }

    public void setCostPerUnit(Double costPerUnit) {
        this.costPerUnit = costPerUnit;
    }

    public Integer getWeight() {
        return weight;
    }

    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "ApiInstanceMonitoringDTO{" +
                "instanceId='" + instanceId + '\'' +
                ", projectId='" + projectId + '\'' +
                ", projectName='" + projectName + '\'' +
                ", businessId='" + businessId + '\'' +
                ", apiIdentifier='" + apiIdentifier + '\'' +
                ", apiType=" + apiType +
                ", status=" + status +
                ", gatewayStatus=" + gatewayStatus +
                ", successRate=" + successRate +
                ", failureRate=" + failureRate +
                ", averageLatency=" + averageLatency +
                ", concurrency=" + concurrency +
                ", recentCalls=" + recentCalls +
                ", successCount=" + successCount +
                ", failureCount=" + failureCount +
                ", lastReportedAt=" + lastReportedAt +
                ", priority=" + priority +
                ", costPerUnit=" + costPerUnit +
                ", weight=" + weight +
                '}';
    }
} 