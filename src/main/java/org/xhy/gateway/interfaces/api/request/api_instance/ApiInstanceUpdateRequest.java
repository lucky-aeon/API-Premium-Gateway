package org.xhy.gateway.interfaces.api.request.api_instance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;

import java.util.Map;

/**
 * API实例更新请求
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ApiInstanceUpdateRequest {

    @NotBlank(message = "项目ID不能为空")
    private String projectId;

    private String userId;

    @NotBlank(message = "API标识符不能为空")
    private String apiIdentifier;

    @NotNull(message = "API类型不能为空")
    private ApiType apiType;

    @NotBlank(message = "业务ID不能为空")
    private String businessId;

    private Map<String, Object> routingParams;

    private Map<String, Object> metadata;

    public @NotBlank(message = "项目ID不能为空") String getProjectId() {
        return projectId;
    }

    public void setProjectId(@NotBlank(message = "项目ID不能为空") String projectId) {
        this.projectId = projectId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public @NotBlank(message = "API标识符不能为空") String getApiIdentifier() {
        return apiIdentifier;
    }

    public void setApiIdentifier(@NotBlank(message = "API标识符不能为空") String apiIdentifier) {
        this.apiIdentifier = apiIdentifier;
    }

    public @NotNull(message = "API类型不能为空") ApiType getApiType() {
        return apiType;
    }

    public void setApiType(@NotNull(message = "API类型不能为空") ApiType apiType) {
        this.apiType = apiType;
    }

    public @NotBlank(message = "业务ID不能为空") String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(@NotBlank(message = "业务ID不能为空") String businessId) {
        this.businessId = businessId;
    }

    public Map<String, Object> getRoutingParams() {
        return routingParams;
    }

    public void setRoutingParams(Map<String, Object> routingParams) {
        this.routingParams = routingParams;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }
}