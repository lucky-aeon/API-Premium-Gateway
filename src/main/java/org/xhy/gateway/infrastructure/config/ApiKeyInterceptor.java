package org.xhy.gateway.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.xhy.gateway.application.service.AuthenticationAppService;

/**
 * API Key 校验拦截器
 * 统一拦截所有API请求，进行身份验证
 * 遵循DDD架构，基础设施层通过应用层调用领域层
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyInterceptor.class);

    private final AuthenticationAppService authenticationAppService;

    // API Key 请求头名称
    private static final String API_KEY_HEADER = "api-key";

    public ApiKeyInterceptor(AuthenticationAppService authenticationAppService) {
        this.authenticationAppService = authenticationAppService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        logger.debug("拦截请求: {} {}", method, requestURI);

        // OPTIONS 请求跳过（CORS 预检请求）
        if ("OPTIONS".equalsIgnoreCase(method)) {
            logger.debug("跳过OPTIONS请求的API Key校验: {}", requestURI);
            return true;
        }

        // 获取API Key
        String apiKey = extractApiKey(request);
        if (!StringUtils.hasText(apiKey)) {
            logger.warn("请求缺少API Key: {} {}", method, requestURI);
            writeErrorResponse(response, 401, "缺少API Key，请在请求头中提供 " + API_KEY_HEADER);
            return false;
        }

        // 通过应用层服务进行认证
        AuthenticationAppService.AuthenticationResult result = authenticationAppService.authenticate(apiKey);
        
        if (!result.isSuccess()) {
            logger.warn("认证失败: {}, URI: {} {}", result.getMessage(), method, requestURI);
            writeErrorResponse(response, result.getStatusCode(), result.getMessage());
            return false;
        }

        logger.debug("API Key校验通过: {}", apiKey);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) throws Exception {
        // 通过应用层服务清理认证上下文，避免内存泄漏
        authenticationAppService.clearAuthenticationContext();
        logger.debug("API上下文已清理");
    }

    /**
     * 从请求中提取API Key
     */
    private String extractApiKey(HttpServletRequest request) {
        // 优先从请求头获取
        String apiKey = request.getHeader(API_KEY_HEADER);
        
        // 如果请求头没有，尝试从查询参数获取
        if (!StringUtils.hasText(apiKey)) {
            apiKey = request.getParameter("apiKey");
        }
        
        // 如果查询参数也没有，尝试从Authorization头获取（Bearer token格式）
        if (!StringUtils.hasText(apiKey)) {
            String authorization = request.getHeader("Authorization");
            if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
                apiKey = authorization.substring(7);
            }
        }
        
        return apiKey;
    }

    /**
     * 写入错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        
        String errorJson = String.format(
            "{\"code\":%d,\"message\":\"%s\",\"data\":null,\"timestamp\":%d}",
            status, message, System.currentTimeMillis()
        );
        
        response.getWriter().write(errorJson);
        response.getWriter().flush();
    }
} 