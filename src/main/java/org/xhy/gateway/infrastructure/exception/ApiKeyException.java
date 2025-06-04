package org.xhy.gateway.infrastructure.exception;

/**
 * API Key 相关异常
 */
public class ApiKeyException extends BusinessException {

    public static final String INVALID_API_KEY = "INVALID_API_KEY";
    public static final String API_KEY_EXPIRED = "API_KEY_EXPIRED";
    public static final String API_KEY_REVOKED = "API_KEY_REVOKED";
    public static final String API_KEY_GENERATION_FAILED = "API_KEY_GENERATION_FAILED";

    public ApiKeyException(String errorCode, String message) {
        super(errorCode, message);
    }

    public ApiKeyException(String errorCode, String message, Throwable cause) {
        super(errorCode, message, cause);
    }

    public static ApiKeyException invalidApiKey() {
        return new ApiKeyException(INVALID_API_KEY, "API Key 无效");
    }

    public static ApiKeyException apiKeyExpired() {
        return new ApiKeyException(API_KEY_EXPIRED, "API Key 已过期");
    }

    public static ApiKeyException apiKeyRevoked() {
        return new ApiKeyException(API_KEY_REVOKED, "API Key 已被撤销");
    }

    public static ApiKeyException generationFailed(String reason) {
        return new ApiKeyException(API_KEY_GENERATION_FAILED, "API Key 生成失败: " + reason);
    }
} 