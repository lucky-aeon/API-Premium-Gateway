package org.xhy.gateway.interfaces.api.request;

import jakarta.validation.constraints.NotBlank;

/**
 * 选择API实例请求
 * 
 * @author xhy
 * @since 1.0.0
 */
public class SelectInstanceRequest {

    /**
     * 用户ID，可选
     */
    private String userId;

    /**
     * API标识符，必填
     */
    @NotBlank(message = "API标识符不能为空")
    private String apiIdentifier;

    /**
     * API类型，必填
     */
    @NotBlank(message = "API类型不能为空")
    private String apiType;

    public SelectInstanceRequest() {
    }


    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getApiIdentifier() {
        return apiIdentifier;
    }

    public void setApiIdentifier(String apiIdentifier) {
        this.apiIdentifier = apiIdentifier;
    }

    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }

    @Override
    public String toString() {
        return "SelectInstanceRequest{" +
                ", userId='" + userId + '\'' +
                ", apiIdentifier='" + apiIdentifier + '\'' +
                ", apiType='" + apiType + '\'' +
                '}';
    }
} 