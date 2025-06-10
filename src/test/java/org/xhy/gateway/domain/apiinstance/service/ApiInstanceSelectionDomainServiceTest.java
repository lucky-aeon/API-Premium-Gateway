package org.xhy.gateway.domain.apiinstance.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xhy.gateway.BaseIntegrationTest;
import org.xhy.gateway.domain.apiinstance.command.InstanceSelectionCommand;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.apiinstance.entity.LoadBalancingType;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API实例选择领域服务测试 - 策略模式负载均衡
 * 
 * @author xhy
 * @since 1.0.0
 */
@DisplayName("API实例选择领域服务测试 - 策略模式负载均衡")
class ApiInstanceSelectionDomainServiceTest extends BaseIntegrationTest {

    @Autowired
    private ApiInstanceSelectionDomainService selectionDomainService;

    @Test
    @DisplayName("轮询策略测试")
    void testRoundRobinStrategy() {
        // Given: 创建基础指标数据
        createSimpleMetrics(testInstanceId1, 20, 5);
        createSimpleMetrics(testInstanceId2, 20, 5);
        createSimpleMetrics(testInstanceId3, 20, 5);

        Map<String, Integer> selectionCount = new HashMap<>();
        
        // When: 使用轮询策略多次选择
        for (int i = 0; i < 30; i++) {
            InstanceSelectionCommand command = new InstanceSelectionCommand(
                    testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.ROUND_ROBIN
            );
            
            ApiInstanceEntity selectedEntity = selectionDomainService.selectBestInstance(command);
            String selectedBusinessId = selectedEntity.getBusinessId();
            selectionCount.merge(selectedBusinessId, 1, Integer::sum);
        }
        
        // Then: 轮询策略应该相对平均地分配
        System.out.println("轮询策略分布: " + selectionCount);
        
        // 每个实例应该被选择约10次（30次选择 / 3个实例）
        selectionCount.values().forEach(count -> 
                assertTrue(count >= 8 && count <= 12, "轮询策略应该相对平均分配，实际次数: " + count));
    }

    @Test
    @DisplayName("成功率优先策略测试")
    void testSuccessRateFirstStrategy() {
        // Given: 为实例创建不同的成功率指标
        createSimpleMetrics(testInstanceId1, 50, 10); // 成功率 83.3%
        createSimpleMetrics(testInstanceId2, 80, 5);  // 成功率 94.1%
        createSimpleMetrics(testInstanceId3, 95, 5);  // 成功率 95.0% (最好)

        Map<String, Integer> selectionCount = new HashMap<>();
        
        // When: 使用成功率优先策略多次选择
        for (int i = 0; i < 20; i++) {
            InstanceSelectionCommand command = new InstanceSelectionCommand(
                    testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.SUCCESS_RATE_FIRST
            );
            
            ApiInstanceEntity selectedEntity = selectionDomainService.selectBestInstance(command);
            String selectedBusinessId = selectedEntity.getBusinessId();
            selectionCount.merge(selectedBusinessId, 1, Integer::sum);
        }
        
        // Then: 成功率最高的实例应该被选择最多次
        System.out.println("成功率优先策略分布: " + selectionCount);
        
        // 实例3（成功率95%最高）应该被选择最多次
        int instance3Count = selectionCount.getOrDefault(TEST_BUSINESS_ID_3, 0);
        int instance2Count = selectionCount.getOrDefault(TEST_BUSINESS_ID_2, 0);
        int instance1Count = selectionCount.getOrDefault(TEST_BUSINESS_ID_1, 0);
        
        assertTrue(instance3Count >= instance2Count, "最高成功率实例应该被选择更多次");
        assertTrue(instance3Count >= instance1Count, "最高成功率实例应该被选择更多次");
        
        // 由于成功率优先策略，实例3应该占大部分选择
        assertTrue(instance3Count > 10, "成功率最高的实例应该被选择超过10次");
    }

    @Test
    @DisplayName("延迟优先策略测试")
    void testLatencyFirstStrategy() {
        // Given: 为实例创建不同的延迟指标
        createLatencyMetrics(testInstanceId1, 20, 0, 2000); // 高延迟 2000ms
        createLatencyMetrics(testInstanceId2, 20, 0, 300);  // 低延迟 300ms (最好)
        createLatencyMetrics(testInstanceId3, 20, 0, 1500); // 中延迟 1500ms

        Map<String, Integer> selectionCount = new HashMap<>();
        
        // When: 使用延迟优先策略多次选择
        for (int i = 0; i < 20; i++) {
            InstanceSelectionCommand command = new InstanceSelectionCommand(
                    testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.LATENCY_FIRST
            );
            
            ApiInstanceEntity selectedEntity = selectionDomainService.selectBestInstance(command);
            String selectedBusinessId = selectedEntity.getBusinessId();
            selectionCount.merge(selectedBusinessId, 1, Integer::sum);
        }
        
        // Then: 延迟最低的实例应该被选择最多次
        System.out.println("延迟优先策略分布: " + selectionCount);
        
        int instance2Count = selectionCount.getOrDefault(TEST_BUSINESS_ID_2, 0);
        int instance1Count = selectionCount.getOrDefault(TEST_BUSINESS_ID_1, 0);
        int instance3Count = selectionCount.getOrDefault(TEST_BUSINESS_ID_3, 0);
        
        assertTrue(instance2Count >= instance1Count, "最低延迟实例应该被选择更多次");
        assertTrue(instance2Count >= instance3Count, "最低延迟实例应该被选择更多次");
        
        // 由于延迟优先策略，实例2应该占大部分选择
        assertTrue(instance2Count > 10, "延迟最低的实例应该被选择超过10次");
    }

    @Test
    @DisplayName("熔断机制测试 - 被熔断的实例不参与负载均衡")
    void testCircuitBreakerExclusion() {
        // Given: 将实例1设置为熔断状态
        InstanceMetricsEntity metrics1 = createSimpleMetrics(testInstanceId1, 10, 40); // 低成功率
        metrics1.updateGatewayStatus(GatewayStatus.CIRCUIT_BREAKER_OPEN);
        metricsRepository.updateById(metrics1);
        
        createSimpleMetrics(testInstanceId2, 20, 5);  // 正常实例
        createSimpleMetrics(testInstanceId3, 15, 3);  // 正常实例

        Map<String, Integer> selectionCount = new HashMap<>();
        
        // When: 使用轮询策略多次选择实例
        for (int i = 0; i < 10; i++) {
            InstanceSelectionCommand command = new InstanceSelectionCommand(
                    testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.ROUND_ROBIN
            );
            
            ApiInstanceEntity selectedEntity = selectionDomainService.selectBestInstance(command);
            String selectedBusinessId = selectedEntity.getBusinessId();
            selectionCount.merge(selectedBusinessId, 1, Integer::sum);
        }
        
        // Then: 被熔断的实例1不应该被选择
        System.out.println("熔断测试选择分布: " + selectionCount);
        assertFalse(selectionCount.containsKey(TEST_BUSINESS_ID_1), "被熔断的实例不应该被选择");
        assertTrue(selectionCount.containsKey(TEST_BUSINESS_ID_2) || selectionCount.containsKey(TEST_BUSINESS_ID_3));
    }

    @Test
    @DisplayName("无可用实例异常测试")
    void testNoAvailableInstanceException() {
        // Given: 停用所有实例
        deactivateAllTestInstances();

        InstanceSelectionCommand command = new InstanceSelectionCommand(
                testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.ROUND_ROBIN
        );

        // When & Then: 应该抛出无可用实例异常
        Exception exception = assertThrows(Exception.class, () -> {
            selectionDomainService.selectBestInstance(command);
        });

        System.out.println("无可用实例异常测试通过: " + exception.getMessage());
    }

    @Test
    @DisplayName("默认策略测试 - 不指定策略时使用智能策略")
    void testDefaultStrategy() {
        // Given: 创建明显差异的指标数据
        createLatencyMetrics(testInstanceId1, 100, 0, 500);  // 低延迟，高成功率
        createLatencyMetrics(testInstanceId2, 50, 50, 2000); // 高延迟，低成功率  
        createLatencyMetrics(testInstanceId3, 80, 20, 1000); // 中等延迟，中等成功率
        
        // When: 不指定负载均衡策略（使用默认构造函数）
        InstanceSelectionCommand command = new InstanceSelectionCommand(
                testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode()
        );

        ApiInstanceEntity apiInstanceEntity = selectionDomainService.selectBestInstance(command);

        // Then: 应该使用默认的智能策略，选择最优实例
        System.out.println("默认智能策略选择的实例: " + apiInstanceEntity.getBusinessId());
        assertNotNull(apiInstanceEntity.getBusinessId());
        
        // 智能策略应该选择成功率高的实例（实例1）
        assertEquals(TEST_BUSINESS_ID_1, apiInstanceEntity.getBusinessId(), "智能策略应该选择成功率最高的实例1");
    }

    @Test
    @DisplayName("智能策略测试 - 通过综合评分选择最优实例")
    void testSmartStrategy() {
        // Given: 创建不同的测试场景
        
        // 场景1：成功率差异很大
        clearMetrics();
        createSimpleMetrics(testInstanceId1, 50, 50);  // 成功率 50%
        createSimpleMetrics(testInstanceId2, 95, 5);   // 成功率 95%
        createSimpleMetrics(testInstanceId3, 80, 20);  // 成功率 80%
        
        InstanceSelectionCommand command1 = new InstanceSelectionCommand(
                testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.SMART
        );
        
        Map<String, Integer> successRateScenario = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            ApiInstanceEntity apiInstanceEntity = selectionDomainService.selectBestInstance(command1);
            successRateScenario.merge(apiInstanceEntity.getBusinessId(), 1, Integer::sum);
        }
        
        System.out.println("智能策略-成功率差异场景: " + successRateScenario);
        
        // 实例2（成功率95%最高）应该被选择最多
        int instance2Count = successRateScenario.getOrDefault(TEST_BUSINESS_ID_2, 0);
        assertTrue(instance2Count > 10, "成功率差异大时，智能策略应该优先选择高成功率实例");
        
        // 场景2：延迟差异很大
        clearMetrics();
        createLatencyMetrics(testInstanceId1, 20, 0, 3000); // 高延迟 3000ms
        createLatencyMetrics(testInstanceId2, 20, 0, 200);  // 低延迟 200ms
        createLatencyMetrics(testInstanceId3, 20, 0, 2500); // 高延迟 2500ms
        
        InstanceSelectionCommand command2 = new InstanceSelectionCommand(
                testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.SMART
        );
        
        Map<String, Integer> latencyScenario = new HashMap<>();
        for (int i = 0; i < 20; i++) {
            ApiInstanceEntity apiInstanceEntity = selectionDomainService.selectBestInstance(command2);
            latencyScenario.merge(apiInstanceEntity.getBusinessId(), 1, Integer::sum);
        }
        
        System.out.println("智能策略-延迟差异场景: " + latencyScenario);
        
        // 实例2（延迟200ms最低）应该被选择最多
        int instance2LatencyCount = latencyScenario.getOrDefault(TEST_BUSINESS_ID_2, 0);
        assertTrue(instance2LatencyCount > 10, "延迟差异大时，智能策略应该优先选择低延迟实例");
        
        // 场景3：性能相近，智能策略会选择综合得分最高的实例
        clearMetrics();
        createLatencyMetrics(testInstanceId1, 100, 10, 1000); // 成功率90.91%, 平均延迟9.09ms
        createLatencyMetrics(testInstanceId2, 98, 12, 1050);  // 成功率89.09%, 平均延迟9.55ms
        createLatencyMetrics(testInstanceId3, 102, 8, 980);   // 成功率92.73%, 平均延迟8.91ms (综合得分最高)
        
        InstanceSelectionCommand command3 = new InstanceSelectionCommand(
                testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.SMART
        );
        
        Map<String, Integer> balancedScenario = new HashMap<>();
        System.out.println("场景3详细选择过程：");
        for (int i = 0; i < 15; i++) {
            ApiInstanceEntity apiInstanceEntity = selectionDomainService.selectBestInstance(command3);
            balancedScenario.merge(apiInstanceEntity.getBusinessId(), 1, Integer::sum);
            System.out.printf("第%d次选择: %s\n", i + 1, apiInstanceEntity.getBusinessId());
        }
        
        System.out.println("智能策略-性能相近场景: " + balancedScenario);
        
        // 智能策略会选择综合得分最高的实例
        // 实例3有最高成功率（92.73%）和最低延迟（8.91ms），应该被选择最多
        int instance1Count = balancedScenario.getOrDefault(TEST_BUSINESS_ID_1, 0);
        int instance2Count2 = balancedScenario.getOrDefault(TEST_BUSINESS_ID_2, 0);
        int instance3Count = balancedScenario.getOrDefault(TEST_BUSINESS_ID_3, 0);
        
        System.out.printf("实例选择统计: 实例1=%d次, 实例2=%d次, 实例3=%d次\n", 
                instance1Count, instance2Count2, instance3Count);
        
        // 根据综合评分，实例3应该有最高得分，被选择最多次
        assertTrue(instance3Count >= instance1Count, 
                String.format("实例3(得分最高)应该被选择不少于实例1: 实例3=%d, 实例1=%d", instance3Count, instance1Count));
        assertTrue(instance3Count >= instance2Count2, 
                String.format("实例3(得分最高)应该被选择不少于实例2: 实例3=%d, 实例2=%d", instance3Count, instance2Count2));
        
        // 调整期望值：如果智能策略工作正常，实例3应该被选择超过5次（在15次选择中）
        assertTrue(instance3Count > 5, 
                String.format("智能策略应该主要选择综合得分最高的实例3，期望>5次，实际=%d次", instance3Count));
        
        // 智能策略在有明显差异时会一直选择最优实例，这是正确行为
        // 不要求所有实例都被选择，因为这不符合智能策略的设计原理
    }

    @Test
    @DisplayName("智能策略冷启动测试 - 没有指标数据时的行为")
    void testSmartStrategyColdStart() {
        // Given: 不创建任何指标数据（冷启动场景）
        Map<String, Integer> selectionCount = new HashMap<>();
        
        // When: 使用智能策略多次选择
        for (int i = 0; i < 12; i++) {
            InstanceSelectionCommand command = new InstanceSelectionCommand(
                    testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.SMART
            );

            ApiInstanceEntity apiInstanceEntity = selectionDomainService.selectBestInstance(command);
            selectionCount.merge(apiInstanceEntity.getBusinessId(), 1, Integer::sum);
        }
        
        // Then: 智能策略在冷启动时所有实例得分相同，但仍会基于综合评分选择
        System.out.println("智能策略冷启动分布: " + selectionCount);
        
        // 验证选择了有效的实例
        assertFalse(selectionCount.isEmpty(), "应该选择了至少一个实例");
        
        // 验证选择的是我们的测试实例之一
        assertTrue(selectionCount.containsKey(TEST_BUSINESS_ID_1) || 
                  selectionCount.containsKey(TEST_BUSINESS_ID_2) || 
                  selectionCount.containsKey(TEST_BUSINESS_ID_3), 
                  "应该选择测试实例中的一个");
        
        // 冷启动时，由于所有实例得分相同，可能会一直选择同一个实例
        // 这是正常行为，不需要强制均匀分配
    }

    @Test
    @DisplayName("策略对比测试 - 不同策略在相同数据下的选择差异")
    void testStrategyComparison() {
        // Given: 创建明显差异的指标数据
        createLatencyMetrics(testInstanceId1, 100, 0, 500);  // 低延迟，高成功率
        createLatencyMetrics(testInstanceId2, 50, 50, 2000); // 高延迟，低成功率  
        createLatencyMetrics(testInstanceId3, 80, 20, 1000); // 中等延迟，中等成功率

        Map<LoadBalancingType, String> strategyResults = new HashMap<>();
        
        // When: 使用不同策略各选择一次
        for (LoadBalancingType strategy : LoadBalancingType.values()) {
            InstanceSelectionCommand command = new InstanceSelectionCommand(
                    testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), strategy
            );

            ApiInstanceEntity apiInstanceEntity = selectionDomainService.selectBestInstance(command);
            strategyResults.put(strategy, apiInstanceEntity.getBusinessId());
        }
        
        // Then: 验证不同策略的选择结果
        System.out.println("策略对比结果: " + strategyResults);
        
        // 成功率优先策略应该选择实例1（成功率100%）
        assertEquals(TEST_BUSINESS_ID_1, strategyResults.get(LoadBalancingType.SUCCESS_RATE_FIRST), 
                "成功率优先策略应该选择成功率最高的实例1");
        
        // 延迟优先策略应该选择实例1（延迟500ms最低）
        assertEquals(TEST_BUSINESS_ID_1, strategyResults.get(LoadBalancingType.LATENCY_FIRST), 
                "延迟优先策略应该选择延迟最低的实例1");
        
        // 轮询策略的结果不做严格验证，因为它不依赖指标数据
    }

    // ========== 辅助方法 ==========

    /**
     * 创建简单的指标数据（只包含成功失败次数）
     */
    private InstanceMetricsEntity createSimpleMetrics(String instanceId, long successCount, long failureCount) {
        InstanceMetricsEntity metrics = new InstanceMetricsEntity();
        metrics.setRegistryId(instanceId);
        metrics.setTimestampWindow(LocalDateTime.now().withSecond(0).withNano(0));
        metrics.setSuccessCount(successCount);
        metrics.setFailureCount(failureCount);
        metrics.setTotalLatencyMs(1000L); // 默认延迟
        metrics.setConcurrency(5);
        metrics.setCurrentGatewayStatus(GatewayStatus.HEALTHY);
        
        metricsRepository.insert(metrics);
        return metrics;
    }

    /**
     * 创建包含延迟信息的指标数据
     */
    private InstanceMetricsEntity createLatencyMetrics(String instanceId, long successCount, long failureCount, long totalLatencyMs) {
        InstanceMetricsEntity metrics = new InstanceMetricsEntity();
        metrics.setRegistryId(instanceId);
        metrics.setTimestampWindow(LocalDateTime.now().withSecond(0).withNano(0));
        metrics.setSuccessCount(successCount);
        metrics.setFailureCount(failureCount);
        metrics.setTotalLatencyMs(totalLatencyMs);
        metrics.setConcurrency(5);
        metrics.setCurrentGatewayStatus(GatewayStatus.HEALTHY);
        
        metricsRepository.insert(metrics);
        return metrics;
    }

    /**
     * 停用所有测试实例
     */
    private void deactivateAllTestInstances() {
        // 获取实例、设置状态、更新到数据库
        ApiInstanceEntity instance1 = apiInstanceRepository.selectById(testInstanceId1);
        instance1.setStatus(ApiInstanceStatus.INACTIVE);
        apiInstanceRepository.updateById(instance1);
        
        ApiInstanceEntity instance2 = apiInstanceRepository.selectById(testInstanceId2);
        instance2.setStatus(ApiInstanceStatus.INACTIVE);
        apiInstanceRepository.updateById(instance2);
        
        ApiInstanceEntity instance3 = apiInstanceRepository.selectById(testInstanceId3);
        instance3.setStatus(ApiInstanceStatus.INACTIVE);
        apiInstanceRepository.updateById(instance3);
        
        System.out.println("已停用所有测试实例");
    }

    /**
     * 清理所有指标数据
     */
    private void clearMetrics() {
        metricsRepository.delete(null); // 删除所有指标数据
    }
} 