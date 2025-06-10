package org.xhy.gateway.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xhy.gateway.BaseIntegrationTest;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;
import org.xhy.gateway.domain.metrics.repository.MetricsRepository;
import org.xhy.gateway.interfaces.api.request.ReportResultRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 选择应用服务测试
 * 重点测试结果上报功能，生成真实的测试数据
 * 
 * @author xhy
 * @since 1.0.0
 */
@DisplayName("选择应用服务测试")
class SelectionAppServiceTest extends BaseIntegrationTest {

    @Autowired
    private SelectionAppService selectionAppService;

    @Autowired
    private MetricsRepository metricsRepository;

    @Test
    @DisplayName("测试成功调用结果上报 - 生成测试数据")
    void testReportSuccessCallResult() {
        // Given: 准备成功调用的上报请求
        ReportResultRequest request = createSuccessReportRequest(testInstanceId1, 800L);

        // When: 上报调用结果
        selectionAppService.reportCallResult(request,this.testProjectId);

        // Then: 验证数据已写入数据库
        InstanceMetricsEntity metrics = findLatestMetrics(testInstanceId1);
        assertNotNull(metrics, "应该创建指标记录");
        assertEquals(1L, metrics.getSuccessCount());
        assertEquals(0L, metrics.getFailureCount());
        assertEquals(800L, metrics.getTotalLatencyMs());
        assertEquals(1.0, metrics.getSuccessRate(), 0.001);
        assertEquals(800.0, metrics.getAverageLatency(), 0.001);
        assertEquals(GatewayStatus.HEALTHY, metrics.getCurrentGatewayStatus());

        System.out.println("✅ 成功上报测试数据: " + metrics);
    }

    @Test
    @DisplayName("测试失败调用结果上报 - 生成测试数据")
    void testReportFailureCallResult() {
        // Given: 准备失败调用的上报请求
        ReportResultRequest request = createFailureReportRequest(
                testInstanceId2, 5000L, "API限流", "RATE_LIMIT");

        // When: 上报调用结果
        selectionAppService.reportCallResult(request,this.testProjectId);

        // Then: 验证数据已写入数据库
        InstanceMetricsEntity metrics = findLatestMetrics(testInstanceId2);
        assertNotNull(metrics, "应该创建指标记录");
        assertEquals(0L, metrics.getSuccessCount());
        assertEquals(1L, metrics.getFailureCount());
        assertEquals(5000L, metrics.getTotalLatencyMs());
        assertEquals(0.0, metrics.getSuccessRate(), 0.001);
        assertEquals(5000.0, metrics.getAverageLatency(), 0.001);

        System.out.println("✅ 失败上报测试数据: " + metrics);
    }

    @Test
    @DisplayName("测试批量上报混合结果 - 生成丰富的测试数据")
    void testBatchReportMixedResults() {
        // Given: 准备多种类型的调用结果
        String instanceId = testInstanceId1;

        // 批量上报：7次成功，3次失败
        System.out.println("=== 开始批量上报测试数据 ===");
        
        // 成功调用
        for (int i = 0; i < 7; i++) {
            ReportResultRequest successRequest = createSuccessReportRequest(
                    instanceId, 500L + (i * 100)); // 延迟递增
            selectionAppService.reportCallResult(successRequest,this.testProjectId);
            System.out.println("上报成功调用 #" + (i + 1));
        }

        // 失败调用
        for (int i = 0; i < 3; i++) {
            ReportResultRequest failureRequest = createFailureReportRequest(
                    instanceId, 3000L + (i * 500), "错误 #" + (i + 1), "API_ERROR");
            selectionAppService.reportCallResult(failureRequest,this.testProjectId);
            System.out.println("上报失败调用 #" + (i + 1));
        }

        // When & Then: 验证聚合数据
        InstanceMetricsEntity metrics = findLatestMetrics(instanceId);
        assertNotNull(metrics, "应该有聚合的指标记录");
        assertEquals(7L, metrics.getSuccessCount());
        assertEquals(3L, metrics.getFailureCount());
        assertEquals(0.7, metrics.getSuccessRate(), 0.01); // 7/10 = 0.7
        assertEquals(GatewayStatus.HEALTHY, metrics.getCurrentGatewayStatus()); // 成功率70%，应该健康

        System.out.println("✅ 批量上报完成，聚合数据: " + metrics);
        System.out.printf("   成功率: %.1f%%, 平均延迟: %.1fms, 总调用: %d次\n", 
                metrics.getSuccessRate(), metrics.getAverageLatency(), metrics.getTotalCount());
    }

    @Test
    @DisplayName("测试高错误率场景 - 触发熔断状态")
    void testHighErrorRateScenario() {
        // Given: 创建高错误率场景 (错误率 > 50%)
        String instanceId = testInstanceId2;

        System.out.println("=== 模拟高错误率场景 ===");

        // 2次成功，8次失败 = 80%错误率
        for (int i = 0; i < 2; i++) {
            ReportResultRequest successRequest = createSuccessReportRequest(instanceId, 600L);
            selectionAppService.reportCallResult(successRequest,this.testProjectId);
        }

        for (int i = 0; i < 8; i++) {
            ReportResultRequest failureRequest = createFailureReportRequest(
                    instanceId, 4000L, "服务异常", "SERVICE_ERROR");
            selectionAppService.reportCallResult(failureRequest,this.testProjectId);
        }

        // When & Then: 验证熔断状态
        InstanceMetricsEntity metrics = findLatestMetrics(instanceId);
        assertNotNull(metrics);
        assertEquals(2L, metrics.getSuccessCount());
        assertEquals(8L, metrics.getFailureCount());
        assertEquals(0.2, metrics.getSuccessRate(), 0.01); // 20%成功率
        assertEquals(GatewayStatus.CIRCUIT_BREAKER_OPEN, metrics.getCurrentGatewayStatus());

        System.out.println("✅ 熔断状态测试数据: " + metrics);
        System.out.printf("   ⚠️ 实例进入熔断状态! 成功率: %.1f%%\n", metrics.getSuccessRate());
    }

    @Test
    @DisplayName("测试高延迟场景 - 触发降级状态")
    void testHighLatencyScenario() {
        // Given: 创建高延迟场景
        String instanceId = testInstanceId3;

        System.out.println("=== 模拟高延迟场景 ===");

        // 10次成功调用，但延迟都很高 (>5000ms)
        for (int i = 0; i < 10; i++) {
            ReportResultRequest request = createSuccessReportRequest(instanceId, 6000L + (i * 100));
            selectionAppService.reportCallResult(request,this.testProjectId);
        }

        // When & Then: 验证降级状态
        InstanceMetricsEntity metrics = findLatestMetrics(instanceId);
        assertNotNull(metrics);
        assertEquals(10L, metrics.getSuccessCount());
        assertEquals(0L, metrics.getFailureCount());
        assertEquals(1.0, metrics.getSuccessRate(), 0.001); // 100%成功率
        assertTrue(metrics.getAverageLatency() > 5000, "平均延迟应该大于5000ms");
        assertEquals(GatewayStatus.DEGRADED, metrics.getCurrentGatewayStatus());

        System.out.println("✅ 降级状态测试数据: " + metrics);
        System.out.printf("   ⚠️ 实例降级! 平均延迟: %.1fms\n", metrics.getAverageLatency());
    }

    @Test
    @DisplayName("测试使用指标上报 - GPT模型调用数据")
    void testUsageMetricsReport() {
        // Given: 准备带使用指标的上报请求
        String instanceId = testInstanceId1;

        System.out.println("=== 模拟GPT模型调用指标上报 ===");

        // 模拟3次GPT调用，每次都有token消耗
        Map<String, Object> usage1 = createGptUsageMetrics(150, 200, 0.0035);
        Map<String, Object> usage2 = createGptUsageMetrics(200, 300, 0.005);
        Map<String, Object> usage3 = createGptUsageMetrics(100, 150, 0.0025);

        ReportResultRequest request1 = createSuccessReportRequestWithUsage(instanceId, 1200L, usage1);
        ReportResultRequest request2 = createSuccessReportRequestWithUsage(instanceId, 1500L, usage2);
        ReportResultRequest request3 = createSuccessReportRequestWithUsage(instanceId, 900L, usage3);

        // When: 上报调用结果
        selectionAppService.reportCallResult(request1,this.testProjectId);
        selectionAppService.reportCallResult(request2,this.testProjectId);
        selectionAppService.reportCallResult(request3,this.testProjectId);

        // Then: 验证使用指标数据
        InstanceMetricsEntity metrics = findLatestMetrics(instanceId);
        assertNotNull(metrics);
        assertEquals(3L, metrics.getSuccessCount());
        
        Map<String, Object> additionalMetrics = metrics.getAdditionalMetrics();
        assertNotNull(additionalMetrics, "应该有使用指标数据");

        System.out.println("✅ 使用指标测试数据: " + metrics);
        System.out.println("   📊 使用指标: " + additionalMetrics);
    }

    // ========== 辅助方法 ==========

    /**
     * 创建成功调用的上报请求
     */
    private ReportResultRequest createSuccessReportRequest(String instanceId, long latencyMs) {
        ReportResultRequest request = new ReportResultRequest();
        request.setInstanceId(instanceId);
        request.setBusinessId(TEST_BUSINESS_ID_1);
        request.setSuccess(true);
        request.setLatencyMs(latencyMs);
        request.setCallTimestamp(System.currentTimeMillis());
        return request;
    }

    /**
     * 创建失败调用的上报请求
     */
    private ReportResultRequest createFailureReportRequest(String instanceId, long latencyMs, 
                                                          String errorMessage, String errorType) {
        ReportResultRequest request = new ReportResultRequest();
        request.setInstanceId(instanceId);
        request.setBusinessId(TEST_BUSINESS_ID_1);
        request.setSuccess(false);
        request.setLatencyMs(latencyMs);
        request.setErrorMessage(errorMessage);
        request.setErrorType(errorType);
        request.setCallTimestamp(System.currentTimeMillis());
        return request;
    }

    /**
     * 创建带使用指标的成功调用上报请求
     */
    private ReportResultRequest createSuccessReportRequestWithUsage(String instanceId, long latencyMs, 
                                                                   Map<String, Object> usageMetrics) {
        ReportResultRequest request = createSuccessReportRequest(instanceId, latencyMs);
        request.setUsageMetrics(usageMetrics);
        return request;
    }

    /**
     * 创建GPT使用指标
     */
    private Map<String, Object> createGptUsageMetrics(int promptTokens, int completionTokens, double totalCost) {
        Map<String, Object> usage = new HashMap<>();
        usage.put("promptTokens", promptTokens);
        usage.put("completionTokens", completionTokens);
        usage.put("totalTokens", promptTokens + completionTokens);
        usage.put("totalCost", totalCost);
        return usage;
    }

    /**
     * 查找实例的最新指标记录
     */
    private InstanceMetricsEntity findLatestMetrics(String instanceId) {
        LocalDateTime currentWindow = LocalDateTime.now().withSecond(0).withNano(0);
        
        return metricsRepository.selectList(null).stream()
                .filter(m -> instanceId.equals(m.getRegistryId()))
                .filter(m -> currentWindow.equals(m.getTimestampWindow()))
                .findFirst()
                .orElse(null);
    }
} 