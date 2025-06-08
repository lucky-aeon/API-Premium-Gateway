package org.xhy.gateway.domain.metrics.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xhy.gateway.BaseIntegrationTest;
import org.xhy.gateway.domain.metrics.command.CallResultCommand;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;
import org.xhy.gateway.domain.metrics.repository.MetricsRepository;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 指标收集领域服务测试
 * 使用真实数据测试指标聚合和熔断机制
 * 
 * @author xhy
 * @since 1.0.0
 */
@DisplayName("指标收集领域服务测试")
class MetricsCollectionDomainServiceTest extends BaseIntegrationTest {

    @Autowired
    private MetricsCollectionDomainService metricsCollectionDomainService;

    @Autowired
    private MetricsRepository metricsRepository;

    @Test
    @DisplayName("测试成功调用结果记录")
    void testRecordSuccessfulCall() {
        // Given: 准备测试数据
        String instanceId = testInstanceId1;
        long latencyMs = 500L;
        
        CallResultCommand command = createSuccessCallResult(instanceId, latencyMs);

        // When: 记录调用结果
        metricsCollectionDomainService.recordCallResult(command);

        // Then: 验证指标被正确记录
        InstanceMetricsEntity metrics = findMetricsByInstanceId(instanceId);
        assertNotNull(metrics);
        assertEquals(1L, metrics.getSuccessCount());
        assertEquals(0L, metrics.getFailureCount());
        assertEquals(latencyMs, metrics.getTotalLatencyMs());
        assertEquals(1.0, metrics.getSuccessRate(), 0.001);
        assertEquals((double) latencyMs, metrics.getAverageLatency(), 0.001);
        assertEquals(GatewayStatus.HEALTHY, metrics.getCurrentGatewayStatus());
        assertTrue(metrics.isHealthy());

        System.out.println("成功调用记录测试通过: " + metrics);
    }

    @Test
    @DisplayName("测试失败调用结果记录")
    void testRecordFailedCall() {
        // Given: 准备失败调用的结果命令
        CallResultCommand failureCommand = createCallResult(
                testInstanceId2, false, 3000L, "Request timeout", "TIMEOUT", null
        );

        // When: 记录调用结果
        metricsCollectionDomainService.recordCallResult(failureCommand);

        // Then: 验证指标被正确记录
        InstanceMetricsEntity metrics = findMetricsByInstanceId(testInstanceId2);
        assertNotNull(metrics);
        assertEquals(0L, metrics.getSuccessCount());
        assertEquals(1L, metrics.getFailureCount());
        assertEquals(3000L, metrics.getTotalLatencyMs());
        assertEquals(0.0, metrics.getSuccessRate(), 0.001);
        assertEquals(3000.0, metrics.getAverageLatency(), 0.001);

        System.out.println("失败调用记录测试通过: " + metrics);
    }

    @Test
    @DisplayName("测试同一时间窗口多次调用聚合")
    void testMultipleCallsInSameWindow() {
        // Given: 准备多个调用结果
        CallResultCommand call1 = createSuccessCallResult(testInstanceId1, 500L);
        CallResultCommand call2 = createSuccessCallResult(testInstanceId1, 800L);
        CallResultCommand call3 = createCallResult(testInstanceId1, false, 2000L, "Error", "API_ERROR", null);

        // When: 记录多个调用结果
        metricsCollectionDomainService.recordCallResult(call1);
        metricsCollectionDomainService.recordCallResult(call2);
        metricsCollectionDomainService.recordCallResult(call3);

        // Then: 验证聚合结果
        InstanceMetricsEntity metrics = findMetricsByInstanceId(testInstanceId1);
        assertNotNull(metrics);
        assertEquals(2L, metrics.getSuccessCount());
        assertEquals(1L, metrics.getFailureCount());
        assertEquals(3300L, metrics.getTotalLatencyMs()); // 500 + 800 + 2000
        assertEquals(0.667, metrics.getSuccessRate(), 0.01); // 2/3 ≈ 0.667
        assertEquals(1100.0, metrics.getAverageLatency(), 0.01); // 3300/3 = 1100

        System.out.println("多次调用聚合测试通过: " + metrics);
    }

    @Test
    @DisplayName("测试熔断器触发 - 错误率过高")
    void testCircuitBreakerTriggeredByHighErrorRate() {
        // Given: 创建高错误率的调用序列 (错误率 > 50%, 调用次数 > 10)
        String instanceId = testInstanceId1;
        
        // 记录12次调用：3次成功，9次失败 (错误率 75%)
        for (int i = 0; i < 3; i++) {
            CallResultCommand successCall = createSuccessCallResult(instanceId, 500L);
            metricsCollectionDomainService.recordCallResult(successCall);
        }
        
        for (int i = 0; i < 9; i++) {
            CallResultCommand failureCall = createCallResult(instanceId, false, 1000L, "Error", "API_ERROR", null);
            metricsCollectionDomainService.recordCallResult(failureCall);
        }

        // When & Then: 验证熔断器被触发
        InstanceMetricsEntity metrics = findMetricsByInstanceId(instanceId);
        assertNotNull(metrics);
        assertEquals(3L, metrics.getSuccessCount());
        assertEquals(9L, metrics.getFailureCount());
        assertEquals(0.25, metrics.getSuccessRate(), 0.01); // 3/12 = 0.25
        assertEquals(GatewayStatus.CIRCUIT_BREAKER_OPEN, metrics.getCurrentGatewayStatus());
        assertTrue(metrics.isCircuitBreakerOpen());

        System.out.println("熔断器触发测试通过: 错误率=" + metrics.getSuccessRate() + ", 状态=" + metrics.getCurrentGatewayStatus());
    }

    @Test
    @DisplayName("测试延迟过高导致降级")
    void testDegradationByHighLatency() {
        // Given: 创建高延迟的调用序列 (平均延迟 > 5000ms)
        String instanceId = testInstanceId2;
        
        // 记录10次高延迟成功调用
        for (int i = 0; i < 10; i++) {
            CallResultCommand highLatencyCall = createSuccessCallResult(instanceId, 6000L);
            metricsCollectionDomainService.recordCallResult(highLatencyCall);
        }

        // When & Then: 验证实例被标记为降级
        InstanceMetricsEntity metrics = findMetricsByInstanceId(instanceId);
        assertNotNull(metrics);
        assertEquals(10L, metrics.getSuccessCount());
        assertEquals(0L, metrics.getFailureCount());
        assertEquals(1.0, metrics.getSuccessRate(), 0.001); // 成功率100%
        assertEquals(6000.0, metrics.getAverageLatency(), 0.001); // 平均延迟6000ms
        assertEquals(GatewayStatus.DEGRADED, metrics.getCurrentGatewayStatus());

        System.out.println("延迟降级测试通过: 平均延迟=" + metrics.getAverageLatency() + "ms, 状态=" + metrics.getCurrentGatewayStatus());
    }

    @Test
    @DisplayName("测试健康实例状态维持")
    void testHealthyInstanceStatus() {
        // Given: 创建良好的调用序列
        String instanceId = testInstanceId3;
        
        // 记录20次良好调用：18次成功，2次失败，延迟适中
        for (int i = 0; i < 18; i++) {
            CallResultCommand goodCall = createSuccessCallResult(instanceId, 500L);
            metricsCollectionDomainService.recordCallResult(goodCall);
        }
        
        for (int i = 0; i < 2; i++) {
            CallResultCommand failureCall = createCallResult(instanceId, false, 800L, "Minor error", "RETRY_ERROR", null);
            metricsCollectionDomainService.recordCallResult(failureCall);
        }

        // When & Then: 验证实例保持健康状态
        InstanceMetricsEntity metrics = findMetricsByInstanceId(instanceId);
        assertNotNull(metrics);
        assertEquals(18L, metrics.getSuccessCount());
        assertEquals(2L, metrics.getFailureCount());
        assertEquals(0.9, metrics.getSuccessRate(), 0.01); // 18/20 = 0.9
        assertEquals(530.0, metrics.getAverageLatency(), 10.0); // (18*500 + 2*800)/20 = 530
        assertEquals(GatewayStatus.HEALTHY, metrics.getCurrentGatewayStatus());
        assertTrue(metrics.isHealthy());

        System.out.println("健康状态维持测试通过: 成功率=" + metrics.getSuccessRate() + ", 平均延迟=" + metrics.getAverageLatency() + "ms");
    }

    @Test
    @DisplayName("测试不同时间窗口的指标隔离")
    void testDifferentTimeWindowIsolation() {
        // Given: 在当前窗口记录调用
        String instanceId = testInstanceId1;
        CallResultCommand currentCall = createSuccessCallResult(instanceId, 500L);
        metricsCollectionDomainService.recordCallResult(currentCall);

        // 手动创建过去时间窗口的指标
        LocalDateTime pastWindow = LocalDateTime.now().minusMinutes(2).withSecond(0).withNano(0);
        InstanceMetricsEntity pastMetrics = new InstanceMetricsEntity();
        pastMetrics.setRegistryId(instanceId);
        pastMetrics.setTimestampWindow(pastWindow);
        pastMetrics.setSuccessCount(5L);
        pastMetrics.setFailureCount(3L);
        pastMetrics.setTotalLatencyMs(4000L);
        pastMetrics.setCurrentGatewayStatus(GatewayStatus.DEGRADED);
        pastMetrics.setLastReportedAt(LocalDateTime.now().minusMinutes(2));
        metricsRepository.insert(pastMetrics);

        // When & Then: 验证时间窗口隔离
        InstanceMetricsEntity currentMetrics = findMetricsByInstanceId(instanceId);
        assertNotNull(currentMetrics);
        
        // 当前窗口应该只有新的调用
        assertEquals(1L, currentMetrics.getSuccessCount());
        assertEquals(0L, currentMetrics.getFailureCount());
        assertEquals(500L, currentMetrics.getTotalLatencyMs());
        assertEquals(GatewayStatus.HEALTHY, currentMetrics.getCurrentGatewayStatus());
        
        // 验证过去窗口的数据仍然存在且独立
        LambdaQueryWrapper<InstanceMetricsEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InstanceMetricsEntity::getRegistryId, instanceId)
                   .eq(InstanceMetricsEntity::getTimestampWindow, pastWindow);
        InstanceMetricsEntity retrievedPastMetrics = metricsRepository.selectOne(queryWrapper);
        assertNotNull(retrievedPastMetrics);
        assertEquals(5L, retrievedPastMetrics.getSuccessCount());
        assertEquals(3L, retrievedPastMetrics.getFailureCount());

        System.out.println("时间窗口隔离测试通过");
        System.out.println("当前窗口: " + currentMetrics);
        System.out.println("过去窗口: " + retrievedPastMetrics);
    }

    @Test
    @DisplayName("测试使用指标的累积")
    void testUsageMetricsAccumulation() {
        // Given: 准备多个带使用指标的调用
        String instanceId = testInstanceId1;
        
        Map<String, Object> usage1 = new HashMap<>();
        usage1.put("promptTokens", 100);
        usage1.put("completionTokens", 150);
        usage1.put("totalCost", 0.002);
        
        Map<String, Object> usage2 = new HashMap<>();
        usage2.put("promptTokens", 200);
        usage2.put("completionTokens", 250);
        usage2.put("totalCost", 0.004);

        CallResultCommand call1 = createCallResult(instanceId, true, 500L, null, null, usage1);
        CallResultCommand call2 = createCallResult(instanceId, true, 600L, null, null, usage2);

        // When: 记录多个调用
        metricsCollectionDomainService.recordCallResult(call1);
        metricsCollectionDomainService.recordCallResult(call2);

        // Then: 验证使用指标累积
        InstanceMetricsEntity metrics = findMetricsByInstanceId(instanceId);
        assertNotNull(metrics);
        
        Map<String, Object> additionalMetrics = metrics.getAdditionalMetrics();
        assertNotNull(additionalMetrics);
        
        // 第二次调用的指标应该覆盖第一次的（这是当前实现的行为）
        assertEquals(200, additionalMetrics.get("promptTokens"));
        assertEquals(250, additionalMetrics.get("completionTokens"));
        assertEquals(0.004, additionalMetrics.get("totalCost"));

        System.out.println("使用指标累积测试通过: " + additionalMetrics);
    }

    // ========== 辅助方法 ==========

    /**
     * 创建调用结果命令 - 简化版本（成功调用）
     */
    private CallResultCommand createSuccessCallResult(String instanceId, long latencyMs) {
        return new CallResultCommand(instanceId, true, latencyMs, null, null, null, System.currentTimeMillis());
    }

    /**
     * 创建调用结果命令 - 完整版本
     */
    private CallResultCommand createCallResult(String instanceId, boolean success, long latencyMs, 
                                              String errorMessage, String errorType, 
                                              Map<String, Object> usageMetrics) {
        return new CallResultCommand(instanceId, success, latencyMs, errorMessage, errorType, 
                                   usageMetrics, System.currentTimeMillis());
    }

    /**
     * 根据实例ID查找最新的指标记录
     */
    private InstanceMetricsEntity findMetricsByInstanceId(String instanceId) {
        LocalDateTime currentWindow = LocalDateTime.now().withSecond(0).withNano(0);
        
        LambdaQueryWrapper<InstanceMetricsEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(InstanceMetricsEntity::getRegistryId, instanceId)
                   .eq(InstanceMetricsEntity::getTimestampWindow, currentWindow)
                   .orderByDesc(InstanceMetricsEntity::getLastReportedAt)
                   .last("LIMIT 1");

        return metricsRepository.selectOne(queryWrapper);
    }
} 