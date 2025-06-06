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
     * 项目ID，必填
     */
    @NotBlank(message = "项目ID不能为空")
    private String projectId;

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

    public SelectInstanceRequest(String projectId, String userId, String apiIdentifier, String apiType) {
        this.projectId = projectId;
        this.userId = userId;
        this.apiIdentifier = apiIdentifier;
        this.apiType = apiType;
    }

    // Getter 和 Setter 方法
    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
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
                "projectId='" + projectId + '\'' +
                ", userId='" + userId + '\'' +
                ", apiIdentifier='" + apiIdentifier + '\'' +
                ", apiType='" + apiType + '\'' +
                '}';
    }
} 