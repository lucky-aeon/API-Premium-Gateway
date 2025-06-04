package org.xhy.gateway.interfaces.api.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建项目请求对象
 * 
 * @author xhy
 * @since 1.0.0
 */
public class ProjectCreateRequest {

    /**
     * 项目名称
     */
    @NotBlank(message = "项目名称不能为空")
    @Size(min = 2, max = 50, message = "项目名称长度必须在2-50个字符之间")
    private String name;

    /**
     * 项目描述
     */
    @Size(max = 200, message = "项目描述长度不能超过200个字符")
    private String description;

    /**
     * API Key
     */
    @NotBlank(message = "API Key不能为空")
    private String apiKey;

    public ProjectCreateRequest() {
    }

    public ProjectCreateRequest(String name, String description, String apiKey) {
        this.name = name;
        this.description = description;
        this.apiKey = apiKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String toString() {
        return "ProjectCreateRequest{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", apiKey='" + apiKey + '\'' +
                '}';
    }
} 