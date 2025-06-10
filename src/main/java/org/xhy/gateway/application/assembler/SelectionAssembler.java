package org.xhy.gateway.application.assembler;

import org.springframework.stereotype.Component;
import org.xhy.gateway.domain.apiinstance.command.InstanceSelectionCommand;
import org.xhy.gateway.domain.apiinstance.entity.AffinityContext;
import org.xhy.gateway.domain.metrics.command.CallResultCommand;
import org.xhy.gateway.interfaces.api.request.ReportResultRequest;
import org.xhy.gateway.interfaces.api.request.SelectInstanceRequest;

/**
 * 选择功能装配器
 * 负责Request对象到领域Command对象的转换
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class SelectionAssembler {

    /**
     * 将SelectInstanceRequest转换为InstanceSelectionCommand
     */
    public InstanceSelectionCommand toCommand(SelectInstanceRequest request, String projectId) {
        if (request == null) {
            return null;
        }

        // 构建亲和性上下文
        AffinityContext affinityContext = null;
        if (request.hasAffinityRequirement()) {
            affinityContext = new AffinityContext(
                request.getAffinityType(),
                request.getAffinityKey()
            );
        }

        return new InstanceSelectionCommand(
                projectId,
                request.getUserId(),
                request.getApiIdentifier(),
                request.getApiType(),
                null, // 使用默认负载均衡策略
                affinityContext
        );
    }

    /**
     * 将ReportResultRequest转换为CallResultCommand
     */
    public CallResultCommand toCommand(ReportResultRequest request, String projectId) {
        if (request == null) {
            return null;
        }

        CallResultCommand callResultCommand = new CallResultCommand(
                request.getInstanceId(),
                request.getSuccess(),
                request.getLatencyMs(),
                request.getErrorMessage(),
                request.getErrorType(),
                request.getUsageMetrics(),
                request.getCallTimestamp()
        );
        callResultCommand.setProjectId(projectId);
        return callResultCommand;
    }
} 