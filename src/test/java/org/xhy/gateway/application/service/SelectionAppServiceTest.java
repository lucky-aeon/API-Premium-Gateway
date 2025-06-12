package org.xhy.gateway.application.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xhy.gateway.BaseIntegrationTest;
import org.xhy.gateway.application.dto.ApiInstanceDTO;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;
import org.xhy.gateway.domain.metrics.repository.MetricsRepository;
import org.xhy.gateway.infrastructure.exception.BusinessException;
import org.xhy.gateway.interfaces.api.request.ReportResultRequest;
import org.xhy.gateway.interfaces.api.request.SelectInstanceRequest;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 选择应用服务测试
 * 重点测试结果上报功能和降级功能，生成真实的测试数据
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
                testInstanceId1, 2000L, "API调用超时", "TIMEOUT_ERROR");

        // When: 上报调用结果
        selectionAppService.reportCallResult(request,this.testProjectId);

        // Then: 验证数据已写入数据库
        InstanceMetricsEntity metrics = findLatestMetrics(testInstanceId1);
        assertNotNull(metrics, "应该创建指标记录");
        assertEquals(0L, metrics.getSuccessCount());
        assertEquals(1L, metrics.getFailureCount());
        assertEquals(2000L, metrics.getTotalLatencyMs());
        assertEquals(0.0, metrics.getSuccessRate(), 0.001);
        assertEquals(2000.0, metrics.getAverageLatency(), 0.001);

        System.out.println("✅ 失败上报测试数据: " + metrics);
    }

    @Test
    @DisplayName("测试混合调用结果上报 - 生成复杂场景数据")
    void testMixedCallResults() {
        // Given: 创建混合调用场景
        String instanceId = testInstanceId1;

        System.out.println("=== 模拟混合调用场景 ===");

        // 7次成功，3次失败 = 70%成功率
        for (int i = 0; i < 7; i++) {
            ReportResultRequest successRequest = createSuccessReportRequest(instanceId, 500L + (i * 50));
            selectionAppService.reportCallResult(successRequest,this.testProjectId);
        }

        for (int i = 0; i < 3; i++) {
            ReportResultRequest failureRequest = createFailureReportRequest(
                    instanceId, 1500L + (i * 200), "间歇性错误", "INTERMITTENT_ERROR");
            selectionAppService.reportCallResult(failureRequest,this.testProjectId);
        }

        // When & Then: 验证混合结果
        InstanceMetricsEntity metrics = findLatestMetrics(instanceId);
        assertNotNull(metrics);
        assertEquals(7L, metrics.getSuccessCount());
        assertEquals(3L, metrics.getFailureCount());
        assertEquals(0.7, metrics.getSuccessRate(), 0.01); // 70%成功率
        assertEquals(GatewayStatus.HEALTHY, metrics.getCurrentGatewayStatus()); // 应该保持健康

        System.out.println("✅ 混合场景测试数据: " + metrics);
        System.out.printf("   📊 成功率: %.1f%%, 平均延迟: %.1fms\n", 
                metrics.getSuccessRate() * 100, metrics.getAverageLatency());
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
        System.out.printf("   ⚠️ 实例进入熔断状态! 成功率: %.1f%%\n", metrics.getSuccessRate() * 100);
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
    @DisplayName("测试降级功能 - 主实例不可用时使用降级链")
    void testFallbackChainFunctionality() {
        System.out.println("=== 测试降级功能 ===");

        // Given: 创建降级实例
        String fallbackBusinessId1 = "gpt4o-fallback-001";
        String fallbackBusinessId2 = "gpt4o-fallback-002";
        
        String fallbackInstanceId1 = createFallbackInstance(fallbackBusinessId1);
        String fallbackInstanceId2 = createFallbackInstance(fallbackBusinessId2);
        
        // 停用主实例，模拟主实例不可用
        deactivateInstance(testInstanceId1);
        deactivateInstance(testInstanceId2);
        deactivateInstance(testInstanceId3);
        
        // 创建带降级链的请求
        SelectInstanceRequest request = new SelectInstanceRequest();
        request.setApiIdentifier(TEST_API_IDENTIFIER);
        request.setApiType(ApiType.MODEL.getCode());
        request.setFallbackChain(Arrays.asList(fallbackBusinessId1, fallbackBusinessId2));

        // When: 选择实例（应该使用降级链）
        ApiInstanceDTO selectedInstance = selectionAppService.selectBestInstance(request, testProjectId);

        // Then: 应该选择到降级实例
        assertNotNull(selectedInstance);
        assertTrue(selectedInstance.getBusinessId().equals(fallbackBusinessId1) || 
                  selectedInstance.getBusinessId().equals(fallbackBusinessId2));
        
        System.out.println("✅ 降级功能测试成功");
        System.out.println("   选择的降级实例: " + selectedInstance.getBusinessId());
        System.out.println("   实例ID: " + selectedInstance.getId());
    }

    @Test
    @DisplayName("测试降级链穷尽 - 所有实例都不可用")
    void testFallbackChainExhausted() {
        System.out.println("=== 测试降级链穷尽场景 ===");

        // Given: 停用所有实例
        deactivateInstance(testInstanceId1);
        deactivateInstance(testInstanceId2);
        deactivateInstance(testInstanceId3);
        
        // 创建带降级链的请求，但降级实例也不存在
        SelectInstanceRequest request = new SelectInstanceRequest();
        request.setApiIdentifier(TEST_API_IDENTIFIER);
        request.setApiType(ApiType.MODEL.getCode());
        request.setFallbackChain(Arrays.asList("non-existent-1", "non-existent-2"));

        // When & Then: 应该抛出降级链穷尽异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            selectionAppService.selectBestInstance(request, testProjectId);
        });

        assertEquals("FALLBACK_EXHAUSTED", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("所有降级实例都不可用"));
        
        System.out.println("✅ 降级链穷尽测试成功");
        System.out.println("   异常信息: " + exception.getMessage());
    }

    @Test
    @DisplayName("测试无降级链时的正常异常处理")
    void testNormalExceptionWithoutFallback() {
        System.out.println("=== 测试无降级链的异常处理 ===");

        // Given: 停用所有实例
        deactivateInstance(testInstanceId1);
        deactivateInstance(testInstanceId2);
        deactivateInstance(testInstanceId3);
        
        // 创建不带降级链的请求
        SelectInstanceRequest request = new SelectInstanceRequest();
        request.setApiIdentifier(TEST_API_IDENTIFIER);
        request.setApiType(ApiType.MODEL.getCode());
        // 不设置降级链

        // When & Then: 应该抛出正常的无可用实例异常
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            selectionAppService.selectBestInstance(request, testProjectId);
        });

        assertEquals("NO_AVAILABLE_INSTANCE", exception.getErrorCode());
        
        System.out.println("✅ 无降级链异常处理测试成功");
        System.out.println("   异常信息: " + exception.getMessage());
    }

    // 辅助方法

    /**
     * 创建降级实例
     */
    private String createFallbackInstance(String businessId) {
        ApiInstanceEntity fallbackInstance = new ApiInstanceEntity();
        fallbackInstance.setProjectId(testProjectId);
        fallbackInstance.setApiIdentifier(businessId); // 使用businessId作为apiIdentifier
        fallbackInstance.setApiType(ApiType.MODEL);
        fallbackInstance.setBusinessId(businessId);
        fallbackInstance.setStatus(ApiInstanceStatus.ACTIVE);
        
        // 设置路由参数
        Map<String, Object> routingParams = new HashMap<>();
        routingParams.put("priority", 50);
        routingParams.put("cost_per_unit", 0.0002);
        routingParams.put("initial_weight", 30);
        fallbackInstance.setRoutingParams(routingParams);
        
        // 设置元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "fallback");
        metadata.put("region", "us-west-1");
        fallbackInstance.setMetadata(metadata);
        
        apiInstanceRepository.insert(fallbackInstance);
        
        System.out.println("   创建降级实例: " + businessId + " -> " + fallbackInstance.getId());
        return fallbackInstance.getId();
    }

    /**
     * 停用实例
     */
    private void deactivateInstance(String instanceId) {
        ApiInstanceEntity instance = apiInstanceRepository.selectById(instanceId);
        if (instance != null) {
            instance.setStatus(ApiInstanceStatus.INACTIVE);
            apiInstanceRepository.updateById(instance);
            System.out.println("   停用实例: " + instanceId + " (" + instance.getBusinessId() + ")");
        }
    }

    /**
     * 创建成功调用上报请求
     */
    private ReportResultRequest createSuccessReportRequest(String instanceId, long latencyMs) {
        ReportResultRequest request = new ReportResultRequest();
        request.setInstanceId(instanceId);
        request.setBusinessId("test-business-id");
        request.setSuccess(true);
        request.setLatencyMs(latencyMs);
        request.setCallTimestamp(System.currentTimeMillis());
        
        // 添加使用指标
        Map<String, Object> usageMetrics = new HashMap<>();
        usageMetrics.put("promptTokens", 100);
        usageMetrics.put("completionTokens", 200);
        usageMetrics.put("totalCost", 0.003);
        request.setUsageMetrics(usageMetrics);
        
        return request;
    }

    /**
     * 创建失败调用上报请求
     */
    private ReportResultRequest createFailureReportRequest(String instanceId, long latencyMs, 
                                                          String errorMessage, String errorType) {
        ReportResultRequest request = new ReportResultRequest();
        request.setInstanceId(instanceId);
        request.setBusinessId("test-business-id");
        request.setSuccess(false);
        request.setLatencyMs(latencyMs);
        request.setErrorMessage(errorMessage);
        request.setErrorType(errorType);
        request.setCallTimestamp(System.currentTimeMillis());
        
        return request;
    }

    /**
     * 查找最新的指标数据
     */
    private InstanceMetricsEntity findLatestMetrics(String instanceId) {
        return metricsRepository.selectList(
                new LambdaQueryWrapper<InstanceMetricsEntity>()
                        .eq(InstanceMetricsEntity::getRegistryId, instanceId)
                        .orderByDesc(InstanceMetricsEntity::getTimestampWindow)
                        .last("LIMIT 1")
        ).stream().findFirst().orElse(null);
    }
} 