package org.xhy.gateway.domain.metrics.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * API实例指标领域实体
 * 对应数据库表：api_instance_metrics
 * 
 * @author xhy
 * @since 1.0.0
 */
@TableName(value = "api_instance_metrics", autoResultMap = true)
public class ApiInstanceMetricsEntity {

    /**
     * 指标记录的唯一标识符 (UUID 字符串，由应用层生成)
     */
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    /**
     * 关联的 API 业务实例 ID，外键关联 api_instance_registry 表
     */
    @TableField("registry_id")
    private String registryId;

    /**
     * 指标统计的时间窗口起始点，例如，记录从该时间点开始的 1 分钟内的聚合数据
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
     * 该时间窗口内所有 API 调用的总延迟（毫秒），用于计算平均延迟
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
    @TableField(value = "last_reported_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime lastReportedAt;

    /**
     * 额外指标，JSONB 格式
     * 例如：{"total_prompt_tokens": 12345, "total_completion_tokens": 67890, "total_cost": 0.123}
     */
    @TableField(value = "additional_metrics", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> additionalMetrics;

    // 构造函数
    public ApiInstanceMetricsEntity() {
        this.successCount = 0L;
        this.failureCount = 0L;
        this.totalLatencyMs = 0L;
        this.concurrency = 0;
        this.currentGatewayStatus = GatewayStatus.HEALTHY;
    }

    public ApiInstanceMetricsEntity(String registryId, LocalDateTime timestampWindow) {
        this();
        this.registryId = registryId;
        this.timestampWindow = timestampWindow;
    }

    // 领域行为方法

    /**
     * 记录成功调用
     */
    public void recordSuccess(long latencyMs) {
        this.successCount++;
        this.totalLatencyMs += latencyMs;
    }

    /**
     * 记录失败调用
     */
    public void recordFailure(long latencyMs) {
        this.failureCount++;
        this.totalLatencyMs += latencyMs;
    }

    /**
     * 更新并发数
     */
    public void updateConcurrency(int concurrency) {
        this.concurrency = Math.max(this.concurrency, concurrency);
    }

    /**
     * 更新Gateway状态
     */
    public void updateGatewayStatus(GatewayStatus status) {
        this.currentGatewayStatus = status;
    }

    /**
     * 计算总调用次数
     */
    public Long getTotalCallCount() {
        return this.successCount + this.failureCount;
    }

    /**
     * 计算成功率（百分比）
     */
    public Double getSuccessRate() {
        Long total = getTotalCallCount();
        if (total == 0) {
            return 0.0;
        }
        return (double) this.successCount / total * 100;
    }

    /**
     * 计算失败率（百分比）
     */
    public Double getFailureRate() {
        return 100.0 - getSuccessRate();
    }

    /**
     * 计算平均延迟（毫秒）
     */
    public Double getAverageLatencyMs() {
        Long total = getTotalCallCount();
        if (total == 0) {
            return 0.0;
        }
        return (double) this.totalLatencyMs / total;
    }

    /**
     * 检查API实例是否健康
     */
    public boolean isHealthy() {
        return GatewayStatus.HEALTHY.equals(this.currentGatewayStatus);
    }

    /**
     * 检查是否处于熔断状态
     */
    public boolean isCircuitBreakerOpen() {
        return GatewayStatus.CIRCUIT_BREAKER_OPEN.equals(this.currentGatewayStatus);
    }

    /**
     * 添加额外指标
     */
    public void addAdditionalMetric(String key, Object value) {
        if (this.additionalMetrics == null) {
            this.additionalMetrics = new java.util.HashMap<>();
        }
        this.additionalMetrics.put(key, value);
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
        return "ApiInstanceMetricsEntity{" +
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