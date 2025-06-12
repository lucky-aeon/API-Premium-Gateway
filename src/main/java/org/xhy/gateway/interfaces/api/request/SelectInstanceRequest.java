package org.xhy.gateway.interfaces.api.request;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

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

    /**
     * 亲和性标识，可选
     * 用于亲和性绑定，相同标识的请求会路由到相同实例
     * 例如：会话ID、用户ID、批次ID等
     */
    private String affinityKey;

    /**
     * 亲和性类型，可选
     * 定义亲和性的类型，例如：SESSION、USER、BATCH、REGION等
     */
    private String affinityType;

    /**
     * 降级链，可选
     * 当主要实例都不可用时，按顺序尝试降级到这些业务ID对应的实例
     * 使用相同的apiType和projectId进行查找
     */
    private List<String> fallbackChain;

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

    public String getAffinityKey() {
        return affinityKey;
    }

    public void setAffinityKey(String affinityKey) {
        this.affinityKey = affinityKey;
    }

    public String getAffinityType() {
        return affinityType;
    }

    public void setAffinityType(String affinityType) {
        this.affinityType = affinityType;
    }

    public List<String> getFallbackChain() {
        return fallbackChain;
    }

    public void setFallbackChain(List<String> fallbackChain) {
        this.fallbackChain = fallbackChain;
    }

    /**
     * 检查是否有亲和性要求
     */
    public boolean hasAffinityRequirement() {
        return affinityKey != null && !affinityKey.trim().isEmpty() 
            && affinityType != null && !affinityType.trim().isEmpty();
    }

    /**
     * 检查是否有降级链
     */
    public boolean hasFallbackChain() {
        return fallbackChain != null && !fallbackChain.isEmpty();
    }

    @Override
    public String toString() {
        return "SelectInstanceRequest{" +
                "userId='" + userId + '\'' +
                ", apiIdentifier='" + apiIdentifier + '\'' +
                ", apiType='" + apiType + '\'' +
                ", affinityKey='" + affinityKey + '\'' +
                ", affinityType='" + affinityType + '\'' +
                ", fallbackChain=" + fallbackChain +
                '}';
    }
} 