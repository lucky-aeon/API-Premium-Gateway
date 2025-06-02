package org.xhy.gateway.domain.apiinstance.entity;

/**
 * API类型枚举
 * 
 * @author xhy
 * @since 1.0.0
 */
public enum ApiType {
    
    /**
     * 模型API，如GPT、Claude等
     */
    MODEL("MODEL", "模型API"),
    
    /**
     * 支付网关API
     */
    PAYMENT_GATEWAY("PAYMENT_GATEWAY", "支付网关"),
    
    /**
     * 通知服务API
     */
    NOTIFICATION_SERVICE("NOTIFICATION_SERVICE", "通知服务"),
    
    /**
     * 短信服务API
     */
    SMS_SERVICE("SMS_SERVICE", "短信服务"),
    
    /**
     * 邮件服务API
     */
    EMAIL_SERVICE("EMAIL_SERVICE", "邮件服务"),
    
    /**
     * 文件存储API
     */
    FILE_STORAGE("FILE_STORAGE", "文件存储"),
    
    /**
     * 图像处理API
     */
    IMAGE_PROCESSING("IMAGE_PROCESSING", "图像处理"),
    
    /**
     * 其他类型API
     */
    OTHER("OTHER", "其他");

    private final String code;
    private final String description;

    ApiType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举值
     */
    public static ApiType fromCode(String code) {
        for (ApiType type : ApiType.values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown API type code: " + code);
    }
} 