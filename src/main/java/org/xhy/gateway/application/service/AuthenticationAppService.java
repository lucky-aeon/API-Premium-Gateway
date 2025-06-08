package org.xhy.gateway.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.xhy.gateway.domain.apikey.service.ApiKeyDomainService;
import org.xhy.gateway.domain.project.service.ProjectDomainService;
import org.xhy.gateway.infrastructure.context.ApiContext;

/**
 * 认证应用服务
 * 处理API Key认证相关的应用层逻辑
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class AuthenticationAppService {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationAppService.class);

    private final ApiKeyDomainService apiKeyDomainService;
    private final ProjectDomainService projectDomainService;

    public AuthenticationAppService(ApiKeyDomainService apiKeyDomainService, 
                                   ProjectDomainService projectDomainService) {
        this.apiKeyDomainService = apiKeyDomainService;
        this.projectDomainService = projectDomainService;
    }

    /**
     * 认证结果
     */
    public static class AuthenticationResult {
        private final boolean success;
        private final String message;
        private final String apiKey;
        private final String projectId;
        private final int statusCode;

        public AuthenticationResult(boolean success, String message, String apiKey, String projectId, int statusCode) {
            this.success = success;
            this.message = message;
            this.apiKey = apiKey;
            this.projectId = projectId;
            this.statusCode = statusCode;
        }

        public static AuthenticationResult success(String apiKey, String projectId) {
            return new AuthenticationResult(true, "认证成功", apiKey, projectId, 200);
        }

        public static AuthenticationResult failure(String message, int statusCode) {
            return new AuthenticationResult(false, message, null, null, statusCode);
        }

        // Getters
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getApiKey() { return apiKey; }
        public String getProjectId() { return projectId; }
        public int getStatusCode() { return statusCode; }
    }

    /**
     * 执行API Key认证
     * 包括API Key验证、项目查找和状态检查
     */
    public AuthenticationResult authenticate(String apiKey) {
        logger.debug("开始API Key认证: {}", apiKey);

        try {
            // 1. 检查API Key是否为空
            if (!StringUtils.hasText(apiKey)) {
                logger.warn("API Key为空");
                return AuthenticationResult.failure("API Key是必需的", 401);
            }

            // 2. 验证API Key是否有效
            if (!apiKeyDomainService.isValidApiKey(apiKey)) {
                logger.warn("无效的API Key: {}", apiKey);
                return AuthenticationResult.failure("无效的API Key", 401);
            }

            // 3. 获取API Key关联的项目ID
            String projectId = apiKeyDomainService.getProjectIdByApiKey(apiKey);
            if (!StringUtils.hasText(projectId)) {
                logger.warn("API Key未关联任何项目: {}", apiKey);
                return AuthenticationResult.failure("API Key未关联项目", 403);
            }

            // 4. 验证项目是否活跃
            if (!projectDomainService.isProjectActive(projectId)) {
                logger.warn("项目不活跃或不存在: projectId={}, apiKey={}", projectId, apiKey);
                return AuthenticationResult.failure("项目不活跃或不存在", 403);
            }

            // 5. 认证成功，设置上下文
            ApiContext.setApiKey(apiKey);
            ApiContext.setProjectId(projectId);

            logger.debug("API Key认证成功: apiKey={}, projectId={}", apiKey, projectId);
            return AuthenticationResult.success(apiKey, projectId);

        } catch (Exception e) {
            logger.error("API Key认证过程发生异常: apiKey={}", apiKey, e);
            return AuthenticationResult.failure("认证服务异常", 500);
        }
    }

    /**
     * 清理认证上下文
     */
    public void clearAuthenticationContext() {
        ApiContext.clear();
        logger.debug("认证上下文已清理");
    }

    /**
     * 获取当前认证的API Key
     */
    public String getCurrentApiKey() {
        return ApiContext.getApiKey();
    }

    /**
     * 获取当前认证的项目ID
     */
    public String getCurrentProjectId() {
        return ApiContext.getProjectId();
    }

    /**
     * 检查当前是否已认证
     */
    public boolean isAuthenticated() {
        return ApiContext.hasApiKey() && ApiContext.hasProjectId();
    }
} 