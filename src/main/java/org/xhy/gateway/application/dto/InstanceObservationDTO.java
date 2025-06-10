package org.xhy.gateway.application.dto;

import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;

import java.time.LocalDateTime;

/**
 * 实例观测DTO
 * 专门用于观测页面表格展示的数据结构
 * 
 * @author xhy
 * @since 1.0.0
 */
public class InstanceObservationDTO {

    /**
     * 项目名称
     */
    private String projectName;

    /**
     * API标识符
     */
    private String apiIdentifier;

    /**
     * API类型
     */
    private ApiType apiType;

    /**
     * 业务ID
     */
    private String businessId;

    /**
     * 实例状态
     */
    private ApiInstanceStatus instanceStatus;

    /**
     * 网关状态
     */
    private GatewayStatus gatewayStatus;

    /**
     * 指定时间窗口内的调用量
     */
    private Long callCount;

    /**
     * 指定时间窗口内的成功率（百分比）
     */
    private Double successRate;

    /**
     * 指定时间窗口内的平均延迟（毫秒）
     */
    private Double averageLatency;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    public InstanceObservationDTO() {
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
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

    public ApiInstanceStatus getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(ApiInstanceStatus instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public GatewayStatus getGatewayStatus() {
        return gatewayStatus;
    }

    public void setGatewayStatus(GatewayStatus gatewayStatus) {
        this.gatewayStatus = gatewayStatus;
    }

    public Long getCallCount() {
        return callCount;
    }

    public void setCallCount(Long callCount) {
        this.callCount = callCount;
    }

    public Double getSuccessRate() {
        return successRate;
    }

    public void setSuccessRate(Double successRate) {
        this.successRate = successRate;
    }

    public Double getAverageLatency() {
        return averageLatency;
    }

    public void setAverageLatency(Double averageLatency) {
        this.averageLatency = averageLatency;
    }

    public LocalDateTime getLastActiveTime() {
        return lastActiveTime;
    }

    public void setLastActiveTime(LocalDateTime lastActiveTime) {
        this.lastActiveTime = lastActiveTime;
    }

    @Override
    public String toString() {
        return "InstanceObservationDTO{" +
                "projectName='" + projectName + '\'' +
                ", apiIdentifier='" + apiIdentifier + '\'' +
                ", apiType=" + apiType +
                ", businessId='" + businessId + '\'' +
                ", instanceStatus=" + instanceStatus +
                ", gatewayStatus=" + gatewayStatus +
                ", callCount=" + callCount +
                ", successRate=" + successRate +
                ", averageLatency=" + averageLatency +
                ", lastActiveTime=" + lastActiveTime +
                '}';
    }
} 