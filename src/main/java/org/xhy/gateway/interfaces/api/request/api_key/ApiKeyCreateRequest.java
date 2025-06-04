package org.xhy.gateway.interfaces.api.request.api_key;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

/**
 * API Key 创建请求
 */
public class ApiKeyCreateRequest {

    private String description;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expiresAt;

    public ApiKeyCreateRequest() {
    }

    public ApiKeyCreateRequest(String description, LocalDateTime expiresAt) {
        this.description = description;
        this.expiresAt = expiresAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
} 