package org.xhy.gateway.interfaces.api.request.monitoring;

import jakarta.validation.constraints.NotBlank;

/**
 * 实例观测请求
 * 用于观测页面的实例状态查询
 * 
 * @author xhy
 * @since 1.0.0
 */
public class InstanceObservationRequest {

    /**
     * 时间窗口
     * 支持: 1m, 10m, 30m, 1h, 6h, 24h
     */
    @NotBlank(message = "时间窗口不能为空")
    private String timeWindow;

    /**
     * 项目ID（可选）
     * 如果不指定则查询所有项目的实例
     */
    private String projectId;

    /**
     * 实例状态筛选（可选）
     * ACTIVE, INACTIVE, DEPRECATED
     */
    private String instanceStatus;

    /**
     * 网关状态筛选（可选）
     * HEALTHY, DEGRADED, FAULTY, CIRCUIT_BREAKER_OPEN
     */
    private String gatewayStatus;

    public InstanceObservationRequest() {
    }

    public InstanceObservationRequest(String timeWindow) {
        this.timeWindow = timeWindow;
    }

    public String getTimeWindow() {
        return timeWindow;
    }

    public void setTimeWindow(String timeWindow) {
        this.timeWindow = timeWindow;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getInstanceStatus() {
        return instanceStatus;
    }

    public void setInstanceStatus(String instanceStatus) {
        this.instanceStatus = instanceStatus;
    }

    public String getGatewayStatus() {
        return gatewayStatus;
    }

    public void setGatewayStatus(String gatewayStatus) {
        this.gatewayStatus = gatewayStatus;
    }

    @Override
    public String toString() {
        return "InstanceObservationRequest{" +
                "timeWindow='" + timeWindow + '\'' +
                ", projectId='" + projectId + '\'' +
                ", instanceStatus='" + instanceStatus + '\'' +
                ", gatewayStatus='" + gatewayStatus + '\'' +
                '}';
    }
} 