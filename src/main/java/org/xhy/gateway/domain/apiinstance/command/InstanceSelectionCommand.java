package org.xhy.gateway.domain.apiinstance.command;

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

    public InstanceSelectionCommand(String projectId, String userId, String apiIdentifier, String apiType) {
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

    @Override
    public String toString() {
        return "InstanceSelectionCommand{" +
                "projectId='" + projectId + '\'' +
                ", userId='" + userId + '\'' +
                ", apiIdentifier='" + apiIdentifier + '\'' +
                ", apiType='" + apiType + '\'' +
                '}';
    }
} 