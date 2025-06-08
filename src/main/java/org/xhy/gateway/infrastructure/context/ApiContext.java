package org.xhy.gateway.infrastructure.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * API请求上下文
 * 使用ThreadLocal管理当前请求的API Key和项目ID
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ApiContext {

    private static final Logger logger = LoggerFactory.getLogger(ApiContext.class);

    private static final ThreadLocal<String> API_KEY_HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> PROJECT_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前请求的API Key
     */
    public static void setApiKey(String apiKey) {
        API_KEY_HOLDER.set(apiKey);
        logger.debug("设置API Key到上下文: {}", apiKey);
    }

    /**
     * 获取当前请求的API Key
     */
    public static String getApiKey() {
        return API_KEY_HOLDER.get();
    }

    /**
     * 设置当前请求的项目ID
     */
    public static void setProjectId(String projectId) {
        PROJECT_ID_HOLDER.set(projectId);
        logger.debug("设置项目ID到上下文: {}", projectId);
    }

    /**
     * 获取当前请求的项目ID
     */
    public static String getProjectId() {
        return PROJECT_ID_HOLDER.get();
    }

    /**
     * 清理当前线程的上下文信息
     * 必须在请求结束时调用，避免内存泄漏
     */
    public static void clear() {
        API_KEY_HOLDER.remove();
        PROJECT_ID_HOLDER.remove();
        logger.debug("清理API上下文");
    }

    /**
     * 检查当前上下文是否包含有效的API Key
     */
    public static boolean hasApiKey() {
        return API_KEY_HOLDER.get() != null;
    }

    /**
     * 检查当前上下文是否包含有效的项目ID
     */
    public static boolean hasProjectId() {
        return PROJECT_ID_HOLDER.get() != null;
    }

    /**
     * 获取当前上下文信息（用于调试）
     */
    public static String getContextInfo() {
        return String.format("ApiKey: %s, ProjectId: %s", 
                getApiKey(), getProjectId());
    }
} 