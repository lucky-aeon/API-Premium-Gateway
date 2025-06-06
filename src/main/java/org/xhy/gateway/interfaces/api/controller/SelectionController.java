package org.xhy.gateway.interfaces.api.controller;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.xhy.gateway.application.service.SelectionAppService;
import org.xhy.gateway.interfaces.api.common.Result;
import org.xhy.gateway.interfaces.api.request.ReportResultRequest;
import org.xhy.gateway.interfaces.api.request.SelectInstanceRequest;

/**
 * API实例选择控制器
 * 提供核心的智能调度功能
 * 
 * @author xhy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/selection")
public class SelectionController {

    private static final Logger logger = LoggerFactory.getLogger(SelectionController.class);

    private final SelectionAppService selectionAppService;

    public SelectionController(SelectionAppService selectionAppService) {
        this.selectionAppService = selectionAppService;
    }

    /**
     * 选择最佳API实例
     * 根据调度算法返回最优的businessId
     */
    @PostMapping("/select-instance")
    public Result<String> selectInstance(@Valid @RequestBody SelectInstanceRequest request) {
        logger.info("接收到选择API实例请求: {}", request);

        String businessId = selectionAppService.selectBestInstance(request);

        logger.info("成功选择API实例，businessId: {}", businessId);
        return Result.success("API实例选择成功", businessId);
    }

    /**
     * 上报API调用结果
     * 用于更新实例指标和健康状态
     */
    @PostMapping("/report-result")
    public Result<Void> reportResult(@Valid @RequestBody ReportResultRequest request) {
        logger.info("接收到调用结果上报: 实例ID={}, 成功={}, 延迟={}ms", 
                request.getInstanceId(), request.getSuccess(), request.getLatencyMs());

        selectionAppService.reportCallResult(request);

        logger.debug("调用结果上报成功");
        return Result.success("调用结果上报成功", null);
    }
} 