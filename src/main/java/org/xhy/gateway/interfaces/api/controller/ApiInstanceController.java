package org.xhy.gateway.interfaces.api.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.xhy.gateway.application.dto.ApiInstanceDTO;
import org.xhy.gateway.application.service.ApiInstanceAppService;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.interfaces.api.common.Result;
import org.xhy.gateway.interfaces.api.request.api_instance.ApiInstanceCreateRequest;
import org.xhy.gateway.interfaces.api.request.api_instance.ApiInstanceUpdateRequest;

import jakarta.validation.Valid;
import java.util.List;

/**
 * API实例控制器
 * 提供API实例相关的RESTful接口
 * 
 * @author xhy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/instances")
public class ApiInstanceController {

    private static final Logger logger = LoggerFactory.getLogger(ApiInstanceController.class);

    private final ApiInstanceAppService apiInstanceAppService;

    public ApiInstanceController(ApiInstanceAppService apiInstanceAppService) {
        this.apiInstanceAppService = apiInstanceAppService;
    }

    /**
     * 创建API实例
     */
    @PostMapping
    public Result<ApiInstanceDTO> createApiInstance(@Valid @RequestBody ApiInstanceCreateRequest request) {
        logger.info("接收到创建API实例请求，项目ID: {}，业务ID: {}", request.getProjectId(), request.getBusinessId());
        
        ApiInstanceDTO result = apiInstanceAppService.createApiInstance(request);
        
        logger.info("API实例创建成功，实例ID: {}", result.getId());
        return Result.success("API实例创建成功", result);
    }

    /**
     * 根据ID获取API实例详情
     */
    @GetMapping("/{id}")
    public Result<ApiInstanceDTO> getApiInstanceById(@PathVariable String id) {
        logger.debug("获取API实例详情，实例ID: {}", id);
        
        ApiInstanceDTO result = apiInstanceAppService.getApiInstanceById(id);
        
        return Result.success(result);
    }

    /**
     * 根据项目ID获取API实例列表
     */
    @GetMapping
    public Result<List<ApiInstanceDTO>> getApiInstancesByProjectId(@RequestParam String projectId) {
        logger.debug("获取项目的API实例列表，项目ID: {}", projectId);
        
        List<ApiInstanceDTO> result = apiInstanceAppService.getApiInstancesByProjectId(projectId);
        
        return Result.success(result);
    }

    /**
     * 根据业务ID和项目ID获取API实例
     */
    @GetMapping("/business/{businessId}")
    public Result<ApiInstanceDTO> getApiInstanceByBusinessId(@RequestParam String projectId, 
                                                             @PathVariable String businessId) {
        logger.debug("根据业务ID获取API实例，项目ID: {}，业务ID: {}", projectId, businessId);
        
        ApiInstanceDTO result = apiInstanceAppService.getApiInstanceByBusinessId(projectId, businessId);
        
        return Result.success(result);
    }

    /**
     * 根据状态获取API实例列表
     */
    @GetMapping("/status/{status}")
    public Result<List<ApiInstanceDTO>> getApiInstancesByStatus(@PathVariable ApiInstanceStatus status) {
        logger.debug("获取指定状态的API实例列表，状态: {}", status);
        
        List<ApiInstanceDTO> result = apiInstanceAppService.getApiInstancesByStatus(status);
        
        return Result.success(result);
    }

    /**
     * 根据API类型获取API实例列表
     */
    @GetMapping("/type/{apiType}")
    public Result<List<ApiInstanceDTO>> getApiInstancesByApiType(@RequestParam String projectId,
                                                                 @PathVariable ApiType apiType) {
        logger.debug("根据API类型获取实例列表，项目ID: {}，API类型: {}", projectId, apiType);
        
        List<ApiInstanceDTO> result = apiInstanceAppService.getApiInstancesByApiType(projectId, apiType);
        
        return Result.success(result);
    }

    /**
     * 更新API实例
     */
    @PutMapping("/{id}")
    public Result<ApiInstanceDTO> updateApiInstance(@PathVariable String id,
                                                    @Valid @RequestBody ApiInstanceUpdateRequest request) {
        logger.info("接收到更新API实例请求，实例ID: {}", id);
        
        ApiInstanceDTO result = apiInstanceAppService.updateApiInstance(id, request);
        
        logger.info("API实例更新成功，实例ID: {}", id);
        return Result.success("API实例更新成功", result);
    }

    /**
     * 删除API实例
     */
    @DeleteMapping("/{projectId}/{businessId}/{apiType}")
    public Result<Void> deleteApiInstance(@PathVariable String projectId, @PathVariable String businessId, @PathVariable String apiType) {
        logger.info("接收到删除API实例请求，项目ID: {}, 业务ID: {}, API类型: {}", projectId, businessId, apiType);
      
        apiInstanceAppService.deleteApiInstance(projectId, businessId, ApiType.fromCode(apiType));
        
        return Result.success("API实例删除成功", null);
    }

    /**
     * 激活API实例
     */
    @PostMapping("/{id}/activate")
    public Result<ApiInstanceDTO> activateApiInstance(@PathVariable String id) {
        logger.info("接收到激活API实例请求，实例ID: {}", id);
        
        ApiInstanceDTO result = apiInstanceAppService.activateApiInstance(id);
        
        logger.info("API实例激活成功，实例ID: {}", id);
        return Result.success("API实例激活成功", result);
    }

    /**
     * 停用API实例
     */
    @PostMapping("/{id}/deactivate")
    public Result<ApiInstanceDTO> deactivateApiInstance(@PathVariable String id) {
        logger.info("接收到停用API实例请求，实例ID: {}", id);
        
        ApiInstanceDTO result = apiInstanceAppService.deactivateApiInstance(id);
        
        logger.info("API实例停用成功，实例ID: {}", id);
        return Result.success("API实例停用成功", result);
    }

    /**
     * 标记API实例为已弃用
     */
    @PostMapping("/{id}/deprecate")
    public Result<ApiInstanceDTO> deprecateApiInstance(@PathVariable String id) {
        logger.info("接收到弃用API实例请求，实例ID: {}", id);
        
        ApiInstanceDTO result = apiInstanceAppService.deprecateApiInstance(id);
        
        logger.info("API实例标记为已弃用成功，实例ID: {}", id);
        return Result.success("API实例标记为已弃用成功", result);
    }
} 