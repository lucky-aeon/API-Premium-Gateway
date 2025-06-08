package org.xhy.gateway;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.apiinstance.repository.ApiInstanceRepository;
import org.xhy.gateway.domain.apikey.entity.ApiKeyEntity;
import org.xhy.gateway.domain.apikey.entity.ApiKeyStatus;
import org.xhy.gateway.domain.apikey.repository.ApiKeyRepository;
import org.xhy.gateway.domain.metrics.repository.MetricsRepository;
import org.xhy.gateway.domain.project.entity.ProjectEntity;
import org.xhy.gateway.domain.project.entity.ProjectStatus;
import org.xhy.gateway.domain.project.repository.ProjectRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 集成测试基类
 * 提供真实数据环境和通用测试工具
 * 
 * @author xhy
 * @since 1.0.0
 */
@SpringBootTest(classes = ApiPremiumGatewayApplication.class)
@AutoConfigureWebMvc
@ActiveProfiles("test")
@Transactional // 每个测试方法都在事务中执行，测试完成后自动回滚
public abstract class BaseIntegrationTest {


    @Autowired
    protected ProjectRepository projectRepository;

    @Autowired
    protected ApiKeyRepository apiKeyRepository;

    @Autowired
    protected ApiInstanceRepository apiInstanceRepository;

    @Autowired
    protected MetricsRepository metricsRepository;

    // 测试数据常量
    protected static final String TEST_PROJECT_NAME = "测试项目";
    protected static final String TEST_PROJECT_DESC = "用于单元测试的项目";
    protected static final String TEST_API_KEY = "test-api-key-" + System.currentTimeMillis();
    protected static final String TEST_API_IDENTIFIER = "gpt4o";
    protected static final String TEST_BUSINESS_ID_1 = "gpt4o-instance-001";
    protected static final String TEST_BUSINESS_ID_2 = "gpt4o-instance-002";
    protected static final String TEST_BUSINESS_ID_3 = "gpt4o-instance-003";

    // 测试数据存储
    protected String testProjectId;
    protected String testApiKeyId;
    protected String testInstanceId1;
    protected String testInstanceId2;
    protected String testInstanceId3;

    @BeforeEach
    void setUpTestData() {
        System.out.println("=== 开始准备测试数据 ===");
        
        // 1. 创建测试API Key
        createTestApiKey();
        
        // 2. 创建测试项目
        createTestProject();
        
        // 3. 创建测试API实例
        createTestApiInstances();
        
        System.out.println("=== 测试数据准备完成 ===");
        System.out.println("项目ID: " + testProjectId);
        System.out.println("API Key ID: " + testApiKeyId);
        System.out.println("实例ID1: " + testInstanceId1);
        System.out.println("实例ID2: " + testInstanceId2);
        System.out.println("实例ID3: " + testInstanceId3);
    }

    @AfterEach
    void cleanUpTestData() {
        System.out.println("=== 测试数据将通过事务回滚自动清理 ===");
    }

    /**
     * 创建测试API Key
     */
    private void createTestApiKey() {
        ApiKeyEntity apiKey = new ApiKeyEntity();
        apiKey.setApiKeyValue(TEST_API_KEY);
        apiKey.setDescription("测试专用API Key");
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        apiKey.setIssuedAt(LocalDateTime.now());
        
        apiKeyRepository.insert(apiKey);
        testApiKeyId = apiKey.getId();
        
        System.out.println("创建测试API Key: " + testApiKeyId);
    }

    /**
     * 创建测试项目
     */
    private void createTestProject() {
        ProjectEntity project = new ProjectEntity();
        project.setName(TEST_PROJECT_NAME);
        project.setDescription(TEST_PROJECT_DESC);
        project.setApiKey(TEST_API_KEY);
        project.setStatus(ProjectStatus.ACTIVE);
        
        projectRepository.insert(project);
        testProjectId = project.getId();
        
        System.out.println("创建测试项目: " + testProjectId);
    }

    /**
     * 创建测试API实例 - 简化版本，不使用复杂路由参数
     */
    private void createTestApiInstances() {
        // 实例1: 简单的GPT-4o实例1
        testInstanceId1 = createApiInstance(TEST_BUSINESS_ID_1, ApiInstanceStatus.ACTIVE);
        
        // 实例2: 简单的GPT-4o实例2
        testInstanceId2 = createApiInstance(TEST_BUSINESS_ID_2, ApiInstanceStatus.ACTIVE);
        
        // 实例3: 简单的GPT-4o实例3
        testInstanceId3 = createApiInstance(TEST_BUSINESS_ID_3, ApiInstanceStatus.ACTIVE);
        
        System.out.println("创建3个相同的GPT-4o实例用于负载均衡测试");
    }

    /**
     * 创建单个API实例 - 提供给子类使用（简化版本）
     */
    protected String createApiInstance(String businessId, ApiInstanceStatus status) {
        ApiInstanceEntity instance = new ApiInstanceEntity();
        instance.setProjectId(testProjectId);
        instance.setApiIdentifier(TEST_API_IDENTIFIER);
        instance.setApiType(ApiType.MODEL);
        instance.setBusinessId(businessId);
        instance.setStatus(status);
        
        // 设置简单的元数据，不包含复杂的路由参数
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", businessId.split("-")[0]);
        metadata.put("region", "us-east-1");
        instance.setMetadata(metadata);
        
        apiInstanceRepository.insert(instance);
        return instance.getId();
    }

    /**
     * 创建单个API实例 - 带路由参数的完整版本（为兼容性保留）
     */
    protected String createApiInstance(String businessId, Map<String, Object> routingParams, ApiInstanceStatus status) {
        ApiInstanceEntity instance = new ApiInstanceEntity();
        instance.setProjectId(testProjectId);
        instance.setApiIdentifier(TEST_API_IDENTIFIER);
        instance.setApiType(ApiType.MODEL);
        instance.setBusinessId(businessId);
        instance.setRoutingParams(routingParams);
        instance.setStatus(status);
        
        // 设置元数据
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("provider", businessId.split("-")[0]);
        metadata.put("region", "us-east-1");
        instance.setMetadata(metadata);
        
        apiInstanceRepository.insert(instance);
        return instance.getId();
    }

    /**
     * 创建路由参数 - 提供给子类使用
     */
    protected Map<String, Object> createRoutingParams(int priority, double costPerUnit, int initialWeight) {
        Map<String, Object> params = new HashMap<>();
        params.put("priority", priority);
        params.put("cost_per_unit", costPerUnit);
        params.put("initial_weight", initialWeight);
        return params;
    }

    /**
     * 生成随机字符串
     */
    protected String randomString() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * 等待一段时间，用于测试时间相关的逻辑
     */
    protected void waitFor(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 创建调用结果命令的辅助方法
     */
    protected org.xhy.gateway.domain.metrics.command.CallResultCommand createCallResultCommand(
            String instanceId, boolean success, long latencyMs, 
            String errorMessage, String errorType, Map<String, Object> usageMetrics) {
        return new org.xhy.gateway.domain.metrics.command.CallResultCommand(
                instanceId, success, latencyMs, errorMessage, errorType, usageMetrics, System.currentTimeMillis());
    }
} 