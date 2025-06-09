package org.xhy.gateway.domain.metrics.command;

import java.util.Map;

/**
 * 调用结果命令
 * 封装API调用结果上报的参数
 * 
 * @author xhy
 * @since 1.0.0
 */
public class CallResultCommand {

    private String projectId;

    /**
     * 实例ID
     */
    private final String instanceId;

    /**
     * 调用是否成功
     */
    private final Boolean success;

    /**
     * 延迟时间（毫秒）
     */
    private final Long latencyMs;

    /**
     * 错误信息
     */
    private final String errorMessage;

    /**
     * 错误类型
     */
    private final String errorType;

    /**
     * 使用指标
     */
    private final Map<String, Object> usageMetrics;

    /**
     * 调用时间戳
     */
    private final Long callTimestamp;

    public CallResultCommand(String instanceId, Boolean success, Long latencyMs, 
                           String errorMessage, String errorType, 
                           Map<String, Object> usageMetrics, Long callTimestamp) {
        this.instanceId = instanceId;
        this.success = success;
        this.latencyMs = latencyMs;
        this.errorMessage = errorMessage;
        this.errorType = errorType;
        this.usageMetrics = usageMetrics;
        this.callTimestamp = callTimestamp;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public Boolean getSuccess() {
        return success;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getErrorType() {
        return errorType;
    }

    public Map<String, Object> getUsageMetrics() {
        return usageMetrics;
    }

    public Long getCallTimestamp() {
        return callTimestamp;
    }

    @Override
    public String toString() {
        return "CallResultCommand{" +
                "instanceId='" + instanceId + '\'' +
                ", success=" + success +
                ", latencyMs=" + latencyMs +
                ", errorMessage='" + errorMessage + '\'' +
                ", errorType='" + errorType + '\'' +
                ", callTimestamp=" + callTimestamp +
                '}';
    }
} 