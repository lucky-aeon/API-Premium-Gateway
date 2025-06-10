package org.xhy.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.xhy.gateway.application.service.SelectionAppService;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.apiinstance.repository.ApiInstanceRepository;
import org.xhy.gateway.domain.apikey.entity.ApiKeyEntity;
import org.xhy.gateway.domain.apikey.entity.ApiKeyStatus;
import org.xhy.gateway.domain.apikey.repository.ApiKeyRepository;
import org.xhy.gateway.domain.project.entity.ProjectEntity;
import org.xhy.gateway.domain.project.entity.ProjectStatus;
import org.xhy.gateway.domain.project.repository.ProjectRepository;
import org.xhy.gateway.interfaces.api.request.ReportResultRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试数据生成器
 * 不使用事务回滚，生成持久化的测试数据
 * 
 * @author xhy
 * @since 1.0.0
 */
@SpringBootTest(classes = ApiPremiumGatewayApplication.class)
@ActiveProfiles("test")
public class TestDataGenerator {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private ApiInstanceRepository apiInstanceRepository;

    @Autowired
    private SelectionAppService selectionAppService;

    // 测试数据常量
    private static final String TEST_API_KEY = "test-data-generator-key-" + System.currentTimeMillis();
    private static final String TEST_PROJECT_NAME = "测试数据生成项目";
    private static final String TEST_PROJECT_DESC = "用于生成持久化测试数据的项目";

    @Test
    public void generateTestData() {
        System.out.println("=== 开始生成持久化测试数据 ===");
        
        // 1. 创建API Key
        String apiKeyId = createApiKey();
        System.out.println("✅ 创建API Key: " + apiKeyId);
        
        // 2. 创建项目
        String projectId = createProject();
        System.out.println("✅ 创建项目: " + projectId);
        
        // 3. 创建API实例
        String instance1 = createApiInstance(projectId, "gpt4o-production-001");
        String instance2 = createApiInstance(projectId, "gpt4o-production-002"); 
        String instance3 = createApiInstance(projectId, "gpt4o-production-003");
        System.out.println("✅ 创建3个API实例");
        
        // 4. 生成各种场景的指标数据
        generateHealthyInstanceMetrics(instance1);
        generateHighErrorRateMetrics(instance2);
        generateHighLatencyMetrics(instance3);
        
        System.out.println("=== 持久化测试数据生成完成 ===");
        System.out.println("项目ID: " + projectId);
        System.out.println("API Key: " + TEST_API_KEY);
        System.out.println("实例1 (健康): " + instance1);
        System.out.println("实例2 (熔断): " + instance2);
        System.out.println("实例3 (降级): " + instance3);
        System.out.println("请查看数据库表 api_instance_metrics 确认数据已保存");
    }

    /**
     * 创建API Key
     */
    private String createApiKey() {
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setApiKeyValue(TEST_API_KEY);
        apiKey.setDescription("测试数据生成器专用API Key");
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        apiKey.setIssuedAt(LocalDateTime.now());
        
        apiKeyRepository.insert(apiKey);
        return apiKey.getId();
    }

    /**
     * 创建项目
     */
    private String createProject() {
        ProjectEntity project = new ProjectEntity();
        project.setName(TEST_PROJECT_NAME);
        project.setDescription(TEST_PROJECT_DESC);
        project.setApiKey(TEST_API_KEY);
        project.setStatus(ProjectStatus.ACTIVE);
        
        projectRepository.insert(project);
        return project.getId();
    }

    /**
     * 创建API实例
     */
    private String createApiInstance(String projectId, String businessId) {
        ApiInstanceEntity instance = new ApiInstanceEntity();
        instance.setProjectId(projectId);
        instance.setApiIdentifier("gpt4o");
        instance.setApiType(ApiType.MODEL);
        instance.setBusinessId(businessId);
        instance.setStatus(ApiInstanceStatus.ACTIVE);
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", "openai");
        metadata.put("region", "us-east-1");
        metadata.put("model", "gpt-4o");
        instance.setMetadata(metadata);
        
        apiInstanceRepository.insert(instance);
        return instance.getId();
    }

    /**
     * 生成健康实例的指标数据
     */
    private void generateHealthyInstanceMetrics(String instanceId) {
        System.out.println("⚡ 生成健康实例指标数据...");
        
        // 生成20次调用：18次成功，2次失败，成功率90%
        for (int i = 0; i < 18; i++) {
            ReportResultRequest request = createSuccessRequest(instanceId, 500 + (i * 50));
            selectionAppService.reportCallResult(request,TEST_PROJECT_NAME);
        }
        
        for (int i = 0; i < 2; i++) {
            ReportResultRequest request = createFailureRequest(instanceId, 1000L, "轻微错误", "RETRY_ERROR");
            selectionAppService.reportCallResult(request,TEST_PROJECT_NAME);
        }
        
        System.out.println("✅ 健康实例数据：20次调用，90%成功率，平均延迟~600ms");
    }

    /**
     * 生成高错误率实例的指标数据 (触发熔断)
     */
    private void generateHighErrorRateMetrics(String instanceId) {
        System.out.println("⚡ 生成高错误率实例指标数据...");
        
        // 生成15次调用：3次成功，12次失败，成功率20%，触发熔断
        for (int i = 0; i < 3; i++) {
            ReportResultRequest request = createSuccessRequest(instanceId, 800L);
            selectionAppService.reportCallResult(request,TEST_PROJECT_NAME);
        }
        
        for (int i = 0; i < 12; i++) {
            ReportResultRequest request = createFailureRequest(instanceId, 5000L, "服务不可用", "SERVICE_ERROR");
            selectionAppService.reportCallResult(request,TEST_PROJECT_NAME);
        }
        
        System.out.println("✅ 高错误率实例数据：15次调用，20%成功率，应触发熔断状态");
    }

    /**
     * 生成高延迟实例的指标数据 (触发降级)
     */
    private void generateHighLatencyMetrics(String instanceId) {
        System.out.println("⚡ 生成高延迟实例指标数据...");
        
        // 生成12次成功调用，但延迟都很高 (>6000ms)，触发降级
        for (int i = 0; i < 12; i++) {
            long latency = 6000L + (i * 200); // 6000ms-8200ms
            ReportResultRequest request = createSuccessRequestWithUsage(
                instanceId, latency, createGptUsageMetrics(150 + i*10, 200 + i*10, 0.003 + i*0.001)
            );
            selectionAppService.reportCallResult(request,TEST_PROJECT_NAME);
        }
        
        System.out.println("✅ 高延迟实例数据：12次调用，100%成功率，平均延迟>6000ms，应触发降级状态");
    }

    /**
     * 创建成功调用请求
     */
    private ReportResultRequest createSuccessRequest(String instanceId, long latencyMs) {
        ReportResultRequest request = new ReportResultRequest();
        request.setInstanceId(instanceId);
        request.setBusinessId("test-business-id");
        request.setSuccess(true);
        request.setLatencyMs(latencyMs);
        request.setCallTimestamp(System.currentTimeMillis());
        return request;
    }

    /**
     * 创建失败调用请求
     */
    private ReportResultRequest createFailureRequest(String instanceId, long latencyMs, 
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
     * 创建带使用指标的成功调用请求
     */
    private ReportResultRequest createSuccessRequestWithUsage(String instanceId, long latencyMs, 
                                                              Map<String, Object> usageMetrics) {
        ReportResultRequest request = createSuccessRequest(instanceId, latencyMs);
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
} 