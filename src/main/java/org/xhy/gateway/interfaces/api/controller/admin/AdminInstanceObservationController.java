package org.xhy.gateway.interfaces.api.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.xhy.gateway.application.dto.InstanceObservationDTO;
import org.xhy.gateway.application.dto.ObservationOverviewDTO;
import org.xhy.gateway.application.service.InstanceObservationAppService;
import org.xhy.gateway.interfaces.api.common.Result;
import org.xhy.gateway.interfaces.api.request.monitoring.InstanceObservationRequest;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 实例观测管理控制器 - 内部管理接口
 * 提供专门用于观测页面的API实例监控功能
 * 
 * @author xhy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/admin/observation")
@Validated
public class AdminInstanceObservationController {

    private static final Logger logger = LoggerFactory.getLogger(AdminInstanceObservationController.class);

    private final InstanceObservationAppService instanceObservationAppService;

    public AdminInstanceObservationController(InstanceObservationAppService instanceObservationAppService) {
        this.instanceObservationAppService = instanceObservationAppService;
    }

    /**
     * 获取观测概览数据
     * 用于页面顶部的统计卡片展示
     */
    @GetMapping("/overview")
    public Result<ObservationOverviewDTO> getObservationOverview(@Valid InstanceObservationRequest request) {
        logger.info("管理后台获取观测概览数据，请求参数: {}", request);
        
        ObservationOverviewDTO result = instanceObservationAppService.getObservationOverview(request);
        
        return Result.success("观测概览数据获取成功", result);
    }

    /**
     * 获取实例观测列表
     * 用于表格展示，支持时间窗口和多种过滤条件
     */
    @GetMapping("/instances")
    public Result<List<InstanceObservationDTO>> getInstanceObservationList(@Valid InstanceObservationRequest request) {
        logger.info("管理后台获取实例观测列表，请求参数: {}", request);
        
        List<InstanceObservationDTO> result = instanceObservationAppService.getInstanceObservationList(request);
        
        logger.info("实例观测列表获取成功，共 {} 个实例", result.size());
        return Result.success("实例观测列表获取成功", result);
    }
} 