package org.xhy.gateway.domain.metrics.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API实例指标实体
 * 对应数据库表：api_instance_metrics
 * 
 * @author xhy
 * @since 1.0.0
 */
@TableName(value = "api_instance_metrics", autoResultMap = true)
public class InstanceMetricsEntity {

    /**
     * 指标记录的唯一标识符
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 关联的 API 业务实例 ID
     */
    @TableField("registry_id")
    private String registryId;

    /**
     * 指标统计的时间窗口起始点
     */
    @TableField("timestamp_window")
    private LocalDateTime timestampWindow;

    /**
     * 该时间窗口内成功的 API 调用次数
     */
    @TableField("success_count")
    private Long successCount;

    /**
     * 该时间窗口内失败的 API 调用次数
     */
    @TableField("failure_count")
    private Long failureCount;

    /**
     * 该时间窗口内所有 API 调用的总延迟（毫秒）
     */
    @TableField("total_latency_ms")
    private Long totalLatencyMs;

    /**
     * 该时间窗口内观察到的最大或当前活跃并发连接数
     */
    @TableField("concurrency")
    private Integer concurrency;

    /**
     * Gateway 根据内部逻辑判断的 API 实例状态
     */
    @TableField("current_gateway_status")
    private GatewayStatus currentGatewayStatus;

    /**
     * 最后一次上报数据到该指标记录的时间
     */
    @TableField("last_reported_at")
    private LocalDateTime lastReportedAt;

    /**
     * 额外指标，JSONB 格式
     */
    @TableField(value = "additional_metrics", typeHandler = JacksonTypeHandler.class, jdbcType = JdbcType.OTHER)
    private Map<String, Object> additionalMetrics;

    // 构造函数
    public InstanceMetricsEntity() {
        this.currentGatewayStatus = GatewayStatus.HEALTHY;
        this.successCount = 0L;
        this.failureCount = 0L;
        this.totalLatencyMs = 0L;
        this.concurrency = 0;
    }

    // 领域行为方法

    /**
     * 计算成功率
     */
    public double getSuccessRate() {
        long total = successCount + failureCount;
        return total == 0 ? 1.0 : (double) successCount / total;
    }

    /**
     * 计算平均延迟
     */
    public double getAverageLatency() {
        long total = successCount + failureCount;
        return total == 0 ? 0.0 : (double) totalLatencyMs / total;
    }

    /**
     * 获取总调用次数
     */
    public long getTotalCount() {
        return successCount + failureCount;
    }

    /**
     * 检查是否健康
     */
    public boolean isHealthy() {
        return GatewayStatus.HEALTHY.equals(currentGatewayStatus);
    }

    /**
     * 检查是否被熔断
     */
    public boolean isCircuitBreakerOpen() {
        return GatewayStatus.CIRCUIT_BREAKER_OPEN.equals(currentGatewayStatus);
    }

    /**
     * 更新网关状态
     */
    public void updateGatewayStatus(GatewayStatus status) {
        this.currentGatewayStatus = status;
    }

    // Getter 和 Setter 方法
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegistryId() {
        return registryId;
    }

    public void setRegistryId(String registryId) {
        this.registryId = registryId;
    }

    public LocalDateTime getTimestampWindow() {
        return timestampWindow;
    }

    public void setTimestampWindow(LocalDateTime timestampWindow) {
        this.timestampWindow = timestampWindow;
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

    public Long getTotalLatencyMs() {
        return totalLatencyMs;
    }

    public void setTotalLatencyMs(Long totalLatencyMs) {
        this.totalLatencyMs = totalLatencyMs;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }

    public GatewayStatus getCurrentGatewayStatus() {
        return currentGatewayStatus;
    }

    public void setCurrentGatewayStatus(GatewayStatus currentGatewayStatus) {
        this.currentGatewayStatus = currentGatewayStatus;
    }

    public LocalDateTime getLastReportedAt() {
        return lastReportedAt;
    }

    public void setLastReportedAt(LocalDateTime lastReportedAt) {
        this.lastReportedAt = lastReportedAt;
    }

    public Map<String, Object> getAdditionalMetrics() {
        return additionalMetrics;
    }

    public void setAdditionalMetrics(Map<String, Object> additionalMetrics) {
        this.additionalMetrics = additionalMetrics;
    }

    @Override
    public String toString() {
        return "InstanceMetricsEntity{" +
                "id='" + id + '\'' +
                ", registryId='" + registryId + '\'' +
                ", timestampWindow=" + timestampWindow +
                ", successCount=" + successCount +
                ", failureCount=" + failureCount +
                ", totalLatencyMs=" + totalLatencyMs +
                ", concurrency=" + concurrency +
                ", currentGatewayStatus=" + currentGatewayStatus +
                ", lastReportedAt=" + lastReportedAt +
                '}';
    }
} 