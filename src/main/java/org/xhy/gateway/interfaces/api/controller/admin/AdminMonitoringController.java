package org.xhy.gateway.interfaces.api.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.xhy.gateway.application.dto.ApiInstanceMonitoringDTO;
import org.xhy.gateway.application.dto.MonitoringOverviewDTO;
import org.xhy.gateway.application.service.MonitoringAppService;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.metrics.entity.GatewayStatus;
import org.xhy.gateway.interfaces.api.common.Result;

import java.util.List;

/**
 * 监控管理控制器 - 内部管理接口
 * 提供API实例监控相关功能，不需要API Key校验
 * 
 * @author xhy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/admin/monitoring")
public class AdminMonitoringController {

    private static final Logger logger = LoggerFactory.getLogger(AdminMonitoringController.class);

    private final MonitoringAppService monitoringAppService;

    public AdminMonitoringController(MonitoringAppService monitoringAppService) {
        this.monitoringAppService = monitoringAppService;
    }

    /**
     * 获取监控概览数据
     * 包含总实例数、健康实例数、平均成功率、平均延迟等核心指标
     */
    @GetMapping("/overview")
    public Result<MonitoringOverviewDTO> getMonitoringOverview(
            @RequestParam(required = false) String projectId) {
        logger.info("管理后台获取监控概览数据，项目ID: {}", projectId);
        
        MonitoringOverviewDTO result = monitoringAppService.getMonitoringOverview(projectId);
        
        return Result.success("监控概览数据获取成功", result);
    }

    /**
     * 获取API实例监控列表
     * 包含实例信息和最新的监控指标数据
     */
    @GetMapping("/instances")
    public Result<List<ApiInstanceMonitoringDTO>> getInstancesWithMetrics(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String gatewayStatus) {
        logger.info("管理后台获取实例监控列表，项目ID: {}，实例状态: {}，网关状态: {}", 
                projectId, status, gatewayStatus);
        
        // 参数转换
        ApiInstanceStatus instanceStatus = null;
        if (status != null && !status.isEmpty()) {
            try {
                instanceStatus = ApiInstanceStatus.valueOf(status);
            } catch (IllegalArgumentException e) {
                logger.warn("无效的实例状态参数: {}", status);
                return Result.badRequest("无效的实例状态参数: " + status);
            }
        }
        
        GatewayStatus gatewareStatus = null;
        if (gatewayStatus != null && !gatewayStatus.isEmpty()) {
            try {
                gatewareStatus = GatewayStatus.valueOf(gatewayStatus);
            } catch (IllegalArgumentException e) {
                logger.warn("无效的网关状态参数: {}", gatewayStatus);
                return Result.badRequest("无效的网关状态参数: " + gatewayStatus);
            }
        }
        
        List<ApiInstanceMonitoringDTO> result = monitoringAppService
                .getInstancesWithMetrics(projectId, instanceStatus, gatewareStatus);
        
        logger.info("实例监控列表获取成功，共 {} 个实例", result.size());
        return Result.success("实例监控列表获取成功", result);
    }

    /**
     * 刷新监控数据
     * 用于手动触发数据刷新
     */
    @PostMapping("/refresh")
    public Result<Void> refreshMonitoringData(@RequestParam(required = false) String projectId) {
        logger.info("管理后台手动刷新监控数据，项目ID: {}", projectId);
        
        // 这里可以添加缓存清理或数据刷新逻辑
        // 目前直接返回成功，因为数据是实时查询的
        
        return Result.success("监控数据刷新成功", null);
    }

    /**
     * 获取实例状态统计
     * 按状态分组统计实例数量
     */
    @GetMapping("/statistics/status")
    public Result<Object> getInstanceStatusStatistics(@RequestParam(required = false) String projectId) {
        logger.info("管理后台获取实例状态统计，项目ID: {}", projectId);
        
        // TODO: 可以后续实现按状态分组的统计功能
        return Result.success("实例状态统计获取成功", null);
    }
} 