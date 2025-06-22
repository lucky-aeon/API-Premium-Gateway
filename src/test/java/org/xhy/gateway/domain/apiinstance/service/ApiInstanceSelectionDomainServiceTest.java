package org.xhy.gateway.domain.apiinstance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xhy.gateway.BaseIntegrationTest;
import org.xhy.gateway.domain.apiinstance.command.InstanceSelectionCommand;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.apiinstance.entity.LoadBalancingType;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;
import org.xhy.gateway.domain.metrics.entity.InstanceMetricsEntity;
import org.xhy.gateway.domain.metrics.repository.MetricsRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * API实例选择领域服务测试 - 专注于领域方法测试
 * 测试候选实例查找、健康实例过滤、策略选择等领域方法
 * 
 * @author xhy
 * @since 1.0.0
 */
@DisplayName("API实例选择领域服务测试")
class ApiInstanceSelectionDomainServiceTest extends BaseIntegrationTest {

    @Autowired
    private ApiInstanceSelectionDomainService selectionDomainService;

    @Autowired
    private MetricsRepository metricsRepository;

    @Test
    @DisplayName("候选实例查找测试")
    void testFindCandidateInstances() {
        // Given: 准备测试数据
        InstanceSelectionCommand command = new InstanceSelectionCommand(
                testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.ROUND_ROBIN
        );

        // When: 查找候选实例
        List<ApiInstanceEntity> candidates = selectionDomainService.findCandidateInstances(command);

        // Then: 验证结果
        assertNotNull(candidates);
        assertTrue(candidates.size() >= 3); // 应该找到我们创建的3个测试实例
        
        // 验证所有候选实例都属于正确的项目和API类型
        candidates.forEach(instance -> {
            assertEquals(testProjectId, instance.getProjectId());
            assertEquals(ApiType.MODEL, instance.getApiType());
        });

        System.out.println("候选实例查找测试通过，找到 " + candidates.size() + " 个候选实例");
    }

    @Test
    @DisplayName("健康实例过滤测试")
    void testFilterHealthyInstances() {
        // Given: 准备候选实例
        InstanceSelectionCommand command = new InstanceSelectionCommand(
                testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.ROUND_ROBIN
        );
        List<ApiInstanceEntity> candidates = selectionDomainService.findCandidateInstances(command);
        
        // 创建指标数据，将其中一个实例设为熔断状态
        Map<String, InstanceMetricsEntity> metricsMap = new HashMap<>();
        
        // 第一个实例：健康
        InstanceMetricsEntity healthyMetrics = createSimpleMetrics(candidates.get(0).getId(), 20, 5);
        healthyMetrics.updateGatewayStatus(GatewayStatus.HEALTHY);
        metricsRepository.updateById(healthyMetrics);
        metricsMap.put(candidates.get(0).getId(), healthyMetrics);
        
        // 第二个实例：熔断
        if (candidates.size() > 1) {
            InstanceMetricsEntity circuitBreakerMetrics = createSimpleMetrics(candidates.get(1).getId(), 10, 40);
            circuitBreakerMetrics.updateGatewayStatus(GatewayStatus.CIRCUIT_BREAKER_OPEN);
            metricsRepository.updateById(circuitBreakerMetrics);
            metricsMap.put(candidates.get(1).getId(), circuitBreakerMetrics);
        }

        // When: 过滤健康实例
        List<ApiInstanceEntity> healthyInstances = selectionDomainService.filterHealthyInstances(candidates, metricsMap);

        // Then: 验证结果
        assertNotNull(healthyInstances);
        assertTrue(healthyInstances.size() < candidates.size()); // 应该过滤掉熔断的实例
        
        // 验证被熔断的实例不在健康实例列表中
        if (candidates.size() > 1) {
            boolean containsCircuitBreakerInstance = healthyInstances.stream()
                    .anyMatch(instance -> instance.getId().equals(candidates.get(1).getId()));
            assertFalse(containsCircuitBreakerInstance, "被熔断的实例不应该在健康实例列表中");
        }

        System.out.println("健康实例过滤测试通过，从 " + candidates.size() + " 个候选实例中过滤出 " + healthyInstances.size() + " 个健康实例");
    }

    @Test
    @DisplayName("策略选择测试")
    void testSelectInstanceWithStrategy() {
        // Given: 准备健康实例和指标数据
        InstanceSelectionCommand command = new InstanceSelectionCommand(
                testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.ROUND_ROBIN
        );
        List<ApiInstanceEntity> candidates = selectionDomainService.findCandidateInstances(command);
        
        // 创建指标数据
        Map<String, InstanceMetricsEntity> metricsMap = new HashMap<>();
        for (ApiInstanceEntity candidate : candidates) {
            InstanceMetricsEntity metrics = createSimpleMetrics(candidate.getId(), 20, 5);
            metricsMap.put(candidate.getId(), metrics);
        }
        
        // 过滤健康实例
        List<ApiInstanceEntity> healthyInstances = selectionDomainService.filterHealthyInstances(candidates, metricsMap);
        
        // When: 使用策略选择实例
        ApiInstanceEntity selectedInstance = selectionDomainService.selectInstanceWithStrategy(
                healthyInstances, metricsMap, command);

        // Then: 验证结果
        assertNotNull(selectedInstance);
        assertTrue(healthyInstances.contains(selectedInstance), "选中的实例应该在健康实例列表中");
        assertNotNull(selectedInstance.getBusinessId());

        System.out.println("策略选择测试通过，选中实例: " + selectedInstance.getBusinessId());
    }

    @Test
    @DisplayName("无健康实例异常测试")
    void testNoHealthyInstanceException() {
        // Given: 准备候选实例，但都设置为熔断状态
        InstanceSelectionCommand command = new InstanceSelectionCommand(
                testProjectId, null, TEST_API_IDENTIFIER, ApiType.MODEL.getCode(), LoadBalancingType.ROUND_ROBIN
        );
        List<ApiInstanceEntity> candidates = selectionDomainService.findCandidateInstances(command);
        
        // 创建指标数据，所有实例都设为熔断状态
        Map<String, InstanceMetricsEntity> metricsMap = new HashMap<>();
        for (ApiInstanceEntity candidate : candidates) {
            InstanceMetricsEntity metrics = createSimpleMetrics(candidate.getId(), 10, 40);
            metrics.updateGatewayStatus(GatewayStatus.CIRCUIT_BREAKER_OPEN);
            metricsRepository.updateById(metrics);
            metricsMap.put(candidate.getId(), metrics);
        }
        
        // 过滤健康实例（应该为空）
        List<ApiInstanceEntity> healthyInstances = selectionDomainService.filterHealthyInstances(candidates, metricsMap);

        // When & Then: 尝试选择实例应该抛出异常
        Exception exception = assertThrows(Exception.class, () -> {
            selectionDomainService.selectInstanceWithStrategy(healthyInstances, metricsMap, command);
        });

        assertTrue(exception.getMessage().contains("NO_HEALTHY_INSTANCE") || 
                  exception.getMessage().contains("没有健康的API实例"));
        System.out.println("无健康实例异常测试通过: " + exception.getMessage());
    }

    // ========== 辅助方法 ==========

    /**
     * 创建简单的指标数据
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
} 