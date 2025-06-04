package org.xhy.gateway.interfaces.api.request.api_key;

/**
 * API Key 状态更新请求
 */
public class ApiKeyStatusUpdateRequest {

    private String status;

    public ApiKeyStatusUpdateRequest() {
    }

    public ApiKeyStatusUpdateRequest(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
} 