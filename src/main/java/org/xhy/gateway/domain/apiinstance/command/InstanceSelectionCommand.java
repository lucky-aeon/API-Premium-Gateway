package org.xhy.gateway.domain.apiinstance.command;

import org.xhy.gateway.domain.apiinstance.entity.LoadBalancingType;

/**
 * 实例选择命令
 * 封装API实例选择算法的输入参数
 * 
 * @author xhy
 * @since 1.0.0
 */
public class InstanceSelectionCommand {

    /**
     * 项目ID
     */
    private final String projectId;

    /**
     * 用户ID
     */
    private final String userId;

    /**
     * API标识符
     */
    private final String apiIdentifier;

    /**
     * API类型
     */
    private final String apiType;

    /**
     * 负载均衡策略
     */
    private final LoadBalancingType loadBalancingType = LoadBalancingType.ROUND_ROBIN;

    public InstanceSelectionCommand(String projectId, String userId, String apiIdentifier, String apiType) {
        this(projectId, userId, apiIdentifier, apiType, LoadBalancingType.SMART);
    }

    public InstanceSelectionCommand(String projectId, String userId, String apiIdentifier, String apiType, LoadBalancingType loadBalancingType) {
        this.projectId = projectId;
        this.userId = userId;
        this.apiIdentifier = apiIdentifier;
        this.apiType = apiType;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getUserId() {
        return userId;
    }

    public String getApiIdentifier() {
        return apiIdentifier;
    }

    public String getApiType() {
        return apiType;
    }

    public LoadBalancingType getLoadBalancingType() {
        return loadBalancingType;
    }

    @Override
    public String toString() {
        return "InstanceSelectionCommand{" +
                "projectId='" + projectId + '\'' +
                ", userId='" + userId + '\'' +
                ", apiIdentifier='" + apiIdentifier + '\'' +
                ", apiType='" + apiType + '\'' +
                ", loadBalancingType=" + loadBalancingType +
                '}';
    }
} 