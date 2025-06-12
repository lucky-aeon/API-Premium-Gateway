package org.xhy.gateway.interfaces.api.controller.external;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.xhy.gateway.application.service.SelectionAppService;
import org.xhy.gateway.application.dto.ApiInstanceDTO;
import org.xhy.gateway.interfaces.api.common.Result;
import org.xhy.gateway.interfaces.api.request.ReportResultRequest;
import org.xhy.gateway.interfaces.api.request.SelectInstanceRequest;
import org.xhy.gateway.infrastructure.context.ApiContext;

/**
 * Gateway 对外暴露的API控制器
 * 提供核心的实例选择和状态上报功能
 * 
 * @author xhy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/gateway")
public class GatewayController {

    private static final Logger logger = LoggerFactory.getLogger(GatewayController.class);

    private final SelectionAppService selectionAppService;

    public GatewayController(SelectionAppService selectionAppService) {
        this.selectionAppService = selectionAppService;
    }

    /**
     * 选择最佳API实例
     * 根据调度算法返回最优的API实例信息，支持降级功能
     * 需要API Key校验
     */
    @PostMapping("/select-instance")
    public Result<ApiInstanceDTO> selectInstance(@Valid @RequestBody SelectInstanceRequest request) {
        // 从上下文中获取当前请求的API Key和项目ID
        String currentApiKey = ApiContext.getApiKey();
        String currentProjectId = ApiContext.getProjectId();
        
        if (request.hasFallbackChain()) {
            logger.info("接收到选择API实例请求（含降级链）: {}, API Key: {}, 项目ID: {}, 降级链: {}", 
                    request.getApiIdentifier(), currentApiKey, currentProjectId, request.getFallbackChain());
        } else {
            logger.info("接收到选择API实例请求: {}, API Key: {}, 项目ID: {}", 
                    request, currentApiKey, currentProjectId);
        }

        ApiInstanceDTO selectedInstance = selectionAppService.selectBestInstance(request, currentProjectId);

        logger.info("成功选择API实例，businessId: {}, instanceId: {}", 
                selectedInstance.getBusinessId(), selectedInstance.getId());
        return Result.success("API实例选择成功", selectedInstance);
    }

    /**
     * 上报API调用结果
     * 用于更新实例指标和健康状态
     * 需要API Key校验
     */
    @PostMapping("/report-result")
    public Result<Void> reportResult(@Valid @RequestBody ReportResultRequest request) {
        logger.info("接收到调用结果上报: 实例ID={}, 成功={}, 延迟={}ms", 
                request.getInstanceId(), request.getSuccess(), request.getLatencyMs());

        String projectId = ApiContext.getProjectId();

        selectionAppService.reportCallResult(request,projectId);

        logger.debug("调用结果上报成功");
        return Result.success("调用结果上报成功", null);
    }
} 