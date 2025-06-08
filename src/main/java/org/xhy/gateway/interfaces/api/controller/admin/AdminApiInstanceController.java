package org.xhy.gateway.interfaces.api.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.xhy.gateway.application.dto.ApiInstanceDTO;
import org.xhy.gateway.application.service.ApiInstanceAppService;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.interfaces.api.common.Result;

import java.util.List;

/**
 * API实例管理控制器 - 内部管理接口
 * 提供查询和监控功能，不需要API Key校验
 * 
 * @author xhy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/admin/instances")
public class AdminApiInstanceController {

    private static final Logger logger = LoggerFactory.getLogger(AdminApiInstanceController.class);

    private final ApiInstanceAppService apiInstanceAppService;

    public AdminApiInstanceController(ApiInstanceAppService apiInstanceAppService) {
        this.apiInstanceAppService = apiInstanceAppService;
    }

    /**
     * 根据ID获取API实例详情
     */
    @GetMapping("/{id}")
    public Result<ApiInstanceDTO> getApiInstanceById(@PathVariable String id) {
        logger.debug("管理后台获取API实例详情，实例ID: {}", id);
        
        ApiInstanceDTO result = apiInstanceAppService.getApiInstanceById(id);
        
        return Result.success(result);
    }

    /**
     * 根据项目ID获取API实例列表
     */
    @GetMapping
    public Result<List<ApiInstanceDTO>> getApiInstancesByProjectId(@RequestParam String projectId) {
        logger.debug("管理后台获取项目的API实例列表，项目ID: {}", projectId);
        
        List<ApiInstanceDTO> result = apiInstanceAppService.getApiInstancesByProjectId(projectId);
        
        return Result.success(result);
    }

    /**
     * 根据业务ID和项目ID获取API实例
     */
    @GetMapping("/business/{businessId}")
    public Result<ApiInstanceDTO> getApiInstanceByBusinessId(@RequestParam String projectId, 
                                                             @PathVariable String businessId) {
        logger.debug("管理后台根据业务ID获取API实例，项目ID: {}，业务ID: {}", projectId, businessId);
        
        ApiInstanceDTO result = apiInstanceAppService.getApiInstanceByBusinessId(projectId, businessId);
        
        return Result.success(result);
    }

    /**
     * 根据状态获取API实例列表（监控用）
     */
    @GetMapping("/status/{status}")
    public Result<List<ApiInstanceDTO>> getApiInstancesByStatus(@PathVariable ApiInstanceStatus status) {
        logger.debug("管理后台获取指定状态的API实例列表，状态: {}", status);
        
        List<ApiInstanceDTO> result = apiInstanceAppService.getApiInstancesByStatus(status);
        
        return Result.success(result);
    }

    /**
     * 根据API类型获取API实例列表（监控用）
     */
    @GetMapping("/type/{apiType}")
    public Result<List<ApiInstanceDTO>> getApiInstancesByApiType(@RequestParam String projectId,
                                                                 @PathVariable ApiType apiType) {
        logger.debug("管理后台根据API类型获取实例列表，项目ID: {}，API类型: {}", projectId, apiType);
        
        List<ApiInstanceDTO> result = apiInstanceAppService.getApiInstancesByApiType(projectId, apiType);
        
        return Result.success(result);
    }

    /**
     * 获取所有API实例（监控大屏用）
     */
    @GetMapping("/all")
    public Result<List<ApiInstanceDTO>> getAllApiInstances() {
        logger.debug("管理后台获取所有API实例列表");
        
        // TODO: 实现获取所有实例的方法
        return Result.success("获取所有实例成功", null);
    }

    /**
     * 获取所有API实例（包含项目信息）- 用于管理后台
     */
    @GetMapping("/with-projects")
    public Result<List<ApiInstanceDTO>> getAllInstancesWithProjects(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) ApiInstanceStatus status) {
        logger.debug("管理后台获取所有API实例列表（包含项目信息），项目ID: {}，状态: {}", projectId, status);
        
        List<ApiInstanceDTO> result = apiInstanceAppService.getAllInstancesWithProjects(projectId, status);
        
        return Result.success("获取实例列表成功", result);
    }
} 