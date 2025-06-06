package org.xhy.gateway.interfaces.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 上报调用结果请求
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ReportResultRequest {

    /**
     * 项目ID，必填
     */
    @NotBlank(message = "项目ID不能为空")
    private String projectId;

    /**
     * 用户ID，可选
     */
    private String userId;

    /**
     * API实例ID，必填
     */
    @NotBlank(message = "实例ID不能为空")
    private String instanceId;

    /**
     * 业务ID，必填
     */
    @NotBlank(message = "业务ID不能为空")
    private String businessId;

    /**
     * 调用是否成功，必填
     */
    @NotNull(message = "成功标识不能为空")
    private Boolean success;

    /**
     * 调用延迟（毫秒），必填
     */
    @NotNull(message = "延迟时间不能为空")
    private Long latencyMs;

    /**
     * 错误信息，失败时可选
     */
    private String errorMessage;

    /**
     * 错误类型，失败时可选
     */
    private String errorType;

    /**
     * 使用指标，可选
     * 例如：{"promptTokens": 100, "completionTokens": 200, "totalCost": 0.003}
     */
    private Map<String, Object> usageMetrics;

    /**
     * 调用时间戳，必填
     */
    @NotNull(message = "调用时间戳不能为空")
    private Long callTimestamp;

    public ReportResultRequest() {
    }

    // Getter 和 Setter 方法
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

    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String instanceId) {
        this.instanceId = instanceId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public void setErrorType(String errorType) {
        this.errorType = errorType;
    }

    public Map<String, Object> getUsageMetrics() {
        return usageMetrics;
    }

    public void setUsageMetrics(Map<String, Object> usageMetrics) {
        this.usageMetrics = usageMetrics;
    }

    public Long getCallTimestamp() {
        return callTimestamp;
    }

    public void setCallTimestamp(Long callTimestamp) {
        this.callTimestamp = callTimestamp;
    }

    @Override
    public String toString() {
        return "ReportResultRequest{" +
                "projectId='" + projectId + '\'' +
                ", userId='" + userId + '\'' +
                ", instanceId='" + instanceId + '\'' +
                ", businessId='" + businessId + '\'' +
                ", success=" + success +
                ", latencyMs=" + latencyMs +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorType='" + errorType + '\'' +
                ", callTimestamp=" + callTimestamp +
                '}';
    }
} 