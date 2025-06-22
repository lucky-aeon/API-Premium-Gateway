package org.xhy.gateway.domain.metrics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.gateway.domain.metrics.command.CallResultCommand;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;
import org.xhy.gateway.domain.metrics.repository.MetricsRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.xhy.gateway.domain.apiinstance.service.SelectionConstants.*;

/**
 * 指标收集领域服务
 * 负责处理API调用结果上报和指标聚合
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class MetricsCollectionDomainService {

    private static final Logger logger = LoggerFactory.getLogger(MetricsCollectionDomainService.class);

    private final MetricsRepository metricsRepository;

    public MetricsCollectionDomainService(MetricsRepository metricsRepository) {
        this.metricsRepository = metricsRepository;
    }

    /**
     * 记录API调用结果
     * 
     * @param command 调用结果命令对象
     */
    public void recordCallResult(CallResultCommand command) {
        
        logger.info("开始记录调用结果: {}", command);

        // 获取或创建当前时间窗口的指标记录
        LocalDateTime currentWindow = getCurrentTimeWindow();
        InstanceMetricsEntity metrics = getOrCreateMetrics(command.getInstanceId(), currentWindow);

        // 更新指标
        updateMetrics(metrics, command.getSuccess(), command.getLatencyMs(), command.getUsageMetrics());

        // 更新Gateway状态
        updateGatewayStatus(metrics);

        // 保存指标
        if (metrics.getId() == null) {
            metricsRepository.insert(metrics);
            logger.debug("创建新的指标记录: instanceId={}, window={}", command.getInstanceId(), currentWindow);
        } else {
            metricsRepository.updateById(metrics);
            logger.debug("更新指标记录: instanceId={}, window={}", command.getInstanceId(), currentWindow);
        }

        logger.info("调用结果记录完成: instanceId={}", command.getInstanceId());
    }

    /**
     * 获取当前时间窗口
     * 按分钟截断，例如 2024-01-01 14:30:00
     */
    private LocalDateTime getCurrentTimeWindow() {
        LocalDateTime now = LocalDateTime.now();
        return now.withSecond(0).withNano(0);
    }

    /**
     * 获取或创建指标记录
     */
    private synchronized InstanceMetricsEntity getOrCreateMetrics(String instanceId, LocalDateTime timeWindow) {
        LambdaQueryWrapper<InstanceMetricsEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InstanceMetricsEntity::getRegistryId, instanceId)
                   .eq(InstanceMetricsEntity::getTimestampWindow, timeWindow);

        InstanceMetricsEntity metrics = metricsRepository.selectOne(queryWrapper);
        
        if (metrics == null) {
            // 创建新的指标记录
            metrics = new InstanceMetricsEntity();
            metrics.setRegistryId(instanceId);
            metrics.setTimestampWindow(timeWindow);
            metrics.setLastReportedAt(LocalDateTime.now());
        } else {
            // 更新最后上报时间
            metrics.setLastReportedAt(LocalDateTime.now());
        }

        return metrics;
    }

    /**
     * 更新指标数据
     */
    private void updateMetrics(InstanceMetricsEntity metrics, Boolean success, Long latencyMs, 
                             Map<String, Object> usageMetrics) {
        
        if (success) {
            metrics.setSuccessCount(metrics.getSuccessCount() + 1);
        } else {
            metrics.setFailureCount(metrics.getFailureCount() + 1);
        }

        // 累计延迟
        metrics.setTotalLatencyMs(metrics.getTotalLatencyMs() + latencyMs);

        // 合并使用指标
        if (usageMetrics != null && !usageMetrics.isEmpty()) {
            Map<String, Object> existingMetrics = metrics.getAdditionalMetrics();
            if (existingMetrics == null) {
                metrics.setAdditionalMetrics(usageMetrics);
            } else {
                // 合并指标数据
                existingMetrics.putAll(usageMetrics);
                metrics.setAdditionalMetrics(existingMetrics);
            }
        }
    }

    /**
     * 更新Gateway状态
     * 根据当前指标判断实例健康状况
     */
    private void updateGatewayStatus(InstanceMetricsEntity metrics) {
        double successRate = metrics.getSuccessRate();
        long totalCalls = metrics.getTotalCount();

        // 如果调用次数太少，保持健康状态
        if (totalCalls < CIRCUIT_BREAKER_MIN_REQUEST_COUNT) {
            metrics.updateGatewayStatus(GatewayStatus.HEALTHY);
            return;
        }

        // 判断是否需要熔断
        if (successRate < CIRCUIT_BREAKER_ERROR_RATE_THRESHOLD) {
            logger.warn("实例错误率过高，触发熔断: instanceId={}, successRate={}, totalCalls={}", 
                    metrics.getRegistryId(), successRate, totalCalls);
            metrics.updateGatewayStatus(GatewayStatus.CIRCUIT_BREAKER_OPEN);
            return;
        }

        // 判断是否降级
        double avgLatency = metrics.getAverageLatency();
        if (avgLatency > LATENCY_SCORE_MAX_MS) {
            logger.warn("实例延迟过高，标记为降级: instanceId={}, avgLatency={}ms", 
                    metrics.getRegistryId(), avgLatency);
            metrics.updateGatewayStatus(GatewayStatus.DEGRADED);
            return;
        }

        // 正常健康状态
        metrics.updateGatewayStatus(GatewayStatus.HEALTHY);
    }

    /**
     * 获取实例指标数据
     * 供选择算法使用
     */  
    public Map<String, InstanceMetricsEntity> getInstanceMetrics(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Map.of();
        }

        // 获取最近的指标数据（最近5分钟内）
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(SHORT_TERM_WINDOW_MINUTES);
        
        LambdaQueryWrapper<InstanceMetricsEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(InstanceMetricsEntity::getRegistryId, instanceIds)
                   .ge(InstanceMetricsEntity::getTimestampWindow, cutoffTime)
                   .orderByDesc(InstanceMetricsEntity::getTimestampWindow);

        List<InstanceMetricsEntity> metricsList = metricsRepository.selectList(queryWrapper);
        
        logger.debug("查询到 {} 条指标数据，实例ID数量: {}", metricsList.size(), instanceIds.size());
        
        // 聚合同一实例的指标数据（取最新的）
        return metricsList.stream()
                .collect(Collectors.toMap(
                        InstanceMetricsEntity::getRegistryId,
                        metrics -> metrics,
                        (existing, replacement) -> 
                                existing.getTimestampWindow().isAfter(replacement.getTimestampWindow()) 
                                        ? existing : replacement
                ));
    }
} 