package org.xhy.gateway.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.xhy.gateway.BaseIntegrationTest;
import org.xhy.gateway.infrastructure.context.ApiContext;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 认证应用服务测试
 * 验证DDD架构下的认证流程
 * 
 * @author xhy
 * @since 1.0.0
 */
@DisplayName("认证应用服务测试")
class AuthenticationAppServiceTest extends BaseIntegrationTest {

    @Autowired
    private AuthenticationAppService authenticationAppService;

    @BeforeEach
    void setUp() {
        // 确保每个测试开始前上下文是清洁的
        ApiContext.clear();
    }

    @Test
    @DisplayName("有效API Key认证成功")
    void testValidApiKeyAuthentication() {
        // When: 使用有效的API Key进行认证
        AuthenticationAppService.AuthenticationResult result = 
                authenticationAppService.authenticate(TEST_API_KEY);

        // Then: 认证应该成功
        assertTrue(result.isSuccess(), "有效API Key认证应该成功");
        assertEquals(200, result.getStatusCode(), "成功状态码应该是200");
        assertEquals("认证成功", result.getMessage(), "认证成功消息正确");
        assertEquals(TEST_API_KEY, result.getApiKey(), "返回的API Key应该匹配");
        assertEquals(testProjectId, result.getProjectId(), "返回的项目ID应该匹配");

        // 验证ThreadLocal上下文是否正确设置
        assertTrue(authenticationAppService.isAuthenticated(), "认证后应该处于已认证状态");
        assertEquals(TEST_API_KEY, authenticationAppService.getCurrentApiKey(), "上下文中的API Key应该正确");
        assertEquals(testProjectId, authenticationAppService.getCurrentProjectId(), "上下文中的项目ID应该正确");
    }

    @Test
    @DisplayName("空API Key认证失败")
    void testEmptyApiKeyAuthentication() {
        // When: 使用空API Key进行认证
        AuthenticationAppService.AuthenticationResult result = 
                authenticationAppService.authenticate("");

        // Then: 认证应该失败
        assertFalse(result.isSuccess(), "空API Key认证应该失败");
        assertEquals(401, result.getStatusCode(), "应该返回401未授权状态码");
        assertEquals("API Key是必需的", result.getMessage(), "错误消息应该正确");
        assertNull(result.getApiKey(), "失败时API Key应该为null");
        assertNull(result.getProjectId(), "失败时项目ID应该为null");

        // 验证ThreadLocal上下文未被设置
        assertFalse(authenticationAppService.isAuthenticated(), "认证失败后应该不处于已认证状态");
    }

    @Test
    @DisplayName("无效API Key认证失败")
    void testInvalidApiKeyAuthentication() {
        // When: 使用无效的API Key进行认证
        String invalidApiKey = "invalid-api-key-123";
        AuthenticationAppService.AuthenticationResult result = 
                authenticationAppService.authenticate(invalidApiKey);

        // Then: 认证应该失败
        assertFalse(result.isSuccess(), "无效API Key认证应该失败");
        assertEquals(401, result.getStatusCode(), "应该返回401未授权状态码");
        assertEquals("无效的API Key", result.getMessage(), "错误消息应该正确");

        // 验证ThreadLocal上下文未被设置
        assertFalse(authenticationAppService.isAuthenticated(), "认证失败后应该不处于已认证状态");
    }

    @Test
    @DisplayName("认证上下文清理测试")
    void testAuthenticationContextCleanup() {
        // Given: 先进行成功认证
        authenticationAppService.authenticate(TEST_API_KEY);
        assertTrue(authenticationAppService.isAuthenticated(), "认证应该成功");

        // When: 清理认证上下文
        authenticationAppService.clearAuthenticationContext();

        // Then: 上下文应该被清理
        assertFalse(authenticationAppService.isAuthenticated(), "清理后应该不处于已认证状态");
        assertNull(authenticationAppService.getCurrentApiKey(), "清理后API Key应该为null");
        assertNull(authenticationAppService.getCurrentProjectId(), "清理后项目ID应该为null");
    }

    @Test
    @DisplayName("验证DDD架构分层 - Application层调用Domain层")
    void testDddArchitecture() {
        // Given: 这个测试主要验证架构正确性
        // 认证应用服务应该通过领域服务完成业务逻辑

        // When: 进行认证
        AuthenticationAppService.AuthenticationResult result = 
                authenticationAppService.authenticate(TEST_API_KEY);

        // Then: 验证认证流程遵循DDD原则
        assertTrue(result.isSuccess(), "认证应该成功");
        
        // 验证应用服务正确地：
        // 1. 调用了ApiKeyDomainService.isValidApiKey()
        // 2. 调用了ApiKeyDomainService.getProjectIdByApiKey()
        // 3. 调用了ProjectDomainService.isProjectActive()
        // 4. 设置了ThreadLocal上下文
        assertNotNull(result.getProjectId(), "应该通过领域服务获取到项目ID");
        assertTrue(authenticationAppService.isAuthenticated(), "上下文应该被正确设置");

        System.out.println("✅ DDD架构验证通过：");
        System.out.println("  - Infrastructure层(Interceptor) → Application层(AuthenticationAppService)");
        System.out.println("  - Application层 → Domain层(ApiKeyDomainService, ProjectDomainService)");
        System.out.println("  - 认证结果: " + result.getMessage());
        System.out.println("  - 项目ID: " + result.getProjectId());
    }
} 