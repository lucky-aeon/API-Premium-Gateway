package org.xhy.gateway.interfaces.api.controller.external;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.xhy.gateway.application.dto.ApiInstanceDTO;
import org.xhy.gateway.application.service.ApiInstanceAppService;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.interfaces.api.common.Result;
import org.xhy.gateway.interfaces.api.request.api_instance.ApiInstanceCreateRequest;
import org.xhy.gateway.interfaces.api.request.api_instance.ApiInstanceUpdateRequest;

import jakarta.validation.Valid;

/**
 * API实例控制器 - 对外暴露接口
 * 提供使用方管理API实例的核心功能：创建、更新、删除、状态管理
 * 需要API Key校验
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
     * 使用方通过API Key创建新的API实例
     */
    @PostMapping
    public Result<ApiInstanceDTO> createApiInstance(@Valid @RequestBody ApiInstanceCreateRequest request) {
        logger.info("接收到创建API实例请求，项目ID: {}，业务ID: {}", request.getProjectId(), request.getBusinessId());
        
        ApiInstanceDTO result = apiInstanceAppService.createApiInstance(request);
        
        logger.info("API实例创建成功，实例ID: {}", result.getId());
        return Result.success("API实例创建成功", result);
    }

    /**
     * 更新API实例
     * 使用方更新已有的API实例配置
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
     * 使用方删除不再需要的API实例
     */
    @DeleteMapping("/{projectId}/{businessId}/{apiType}")
    public Result<Void> deleteApiInstance(@PathVariable String projectId, 
                                          @PathVariable String businessId, 
                                          @PathVariable String apiType) {
        logger.info("接收到删除API实例请求，项目ID: {}, 业务ID: {}, API类型: {}", projectId, businessId, apiType);
      
        apiInstanceAppService.deleteApiInstance(projectId, businessId, ApiType.fromCode(apiType));
        
        logger.info("API实例删除成功，项目ID: {}, 业务ID: {}", projectId, businessId);
        return Result.success("API实例删除成功", null);
    }

    /**
     * 激活API实例
     * 使API实例可以参与负载均衡
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
     * 暂停API实例参与负载均衡
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
     * 标记API实例为弃用状态，逐步下线
     */
    @PostMapping("/{id}/deprecate")
    public Result<ApiInstanceDTO> deprecateApiInstance(@PathVariable String id) {
        logger.info("接收到弃用API实例请求，实例ID: {}", id);
        
        ApiInstanceDTO result = apiInstanceAppService.deprecateApiInstance(id);
        
        logger.info("API实例标记为已弃用成功，实例ID: {}", id);
        return Result.success("API实例标记为已弃用成功", result);
    }
} 