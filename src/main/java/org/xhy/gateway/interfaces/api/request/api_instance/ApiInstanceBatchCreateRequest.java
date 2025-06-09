package org.xhy.gateway.interfaces.api.request.api_instance;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * API实例批量创建请求
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ApiInstanceBatchCreateRequest {

    /**
     * 批量创建的API实例列表
     */
    @NotEmpty(message = "API实例列表不能为空")
    private List<ApiInstanceCreateRequest> instances;

    public ApiInstanceBatchCreateRequest() {}

    public List<ApiInstanceCreateRequest> getInstances() {
        return instances;
    }

    public void setInstances(List<ApiInstanceCreateRequest> instances) {
        this.instances = instances;
    }
} 