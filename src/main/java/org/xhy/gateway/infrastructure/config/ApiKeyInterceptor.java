package org.xhy.gateway.infrastructure.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;
import org.xhy.gateway.domain.apikey.service.ApiKeyDomainService;
import org.xhy.gateway.domain.project.service.ProjectDomainService;

/**
 * API Key 校验拦截器
 * 统一拦截所有API请求，进行身份验证
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class ApiKeyInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyInterceptor.class);

    private final ApiKeyDomainService apiKeyDomainService;
    private final ProjectDomainService projectDomainService;

    // API Key 请求头名称
    private static final String API_KEY_HEADER = "X-API-Key";
    // 项目ID请求头名称
    private static final String PROJECT_ID_HEADER = "X-Project-Id";

    public ApiKeyInterceptor(ApiKeyDomainService apiKeyDomainService, ProjectDomainService projectDomainService) {
        this.apiKeyDomainService = apiKeyDomainService;
        this.projectDomainService = projectDomainService;
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
        
        // 所有被拦截的请求都需要API Key校验（已在WebMvcConfig中精确配置拦截路径）

        // 获取API Key
        String apiKey = extractApiKey(request);
        if (!StringUtils.hasText(apiKey)) {
            logger.warn("请求缺少API Key: {} {}", method, requestURI);
            writeErrorResponse(response, 401, "缺少API Key，请在请求头中提供 " + API_KEY_HEADER);
            return false;
        }

        // 校验API Key
        if (!apiKeyDomainService.isUsable(apiKey)) {
            logger.warn("无效的API Key: {}, URI: {} {}", apiKey, method, requestURI);
            writeErrorResponse(response, 401, "无效的API Key或API Key已过期");
            return false;
        }

        // 获取项目ID（可选，某些接口可能需要）
        String projectId = request.getHeader(PROJECT_ID_HEADER);
        if (StringUtils.hasText(projectId)) {
            // 校验项目是否存在且活跃
            if (!projectDomainService.isProjectActive(projectId)) {
                logger.warn("项目不存在或已停用: {}, API Key: {}", projectId, apiKey);
                writeErrorResponse(response, 403, "项目不存在或已停用");
                return false;
            }
            
            // 将项目ID设置到请求属性中，供后续处理使用
            request.setAttribute("projectId", projectId);
        }

        // 将API Key设置到请求属性中，供后续处理使用
        request.setAttribute("apiKey", apiKey);

        logger.debug("API Key校验通过: {}", apiKey);
        return true;
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