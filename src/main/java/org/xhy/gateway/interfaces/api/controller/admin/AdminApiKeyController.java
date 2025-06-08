package org.xhy.gateway.interfaces.api.controller.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.xhy.gateway.application.dto.ApiKeyDTO;
import org.xhy.gateway.application.service.ApiKeyAppService;
import org.xhy.gateway.domain.apikey.entity.ApiKeyStatus;
import org.xhy.gateway.interfaces.api.common.Result;
import org.xhy.gateway.interfaces.api.request.api_key.ApiKeyCreateRequest;
import org.xhy.gateway.interfaces.api.request.api_key.ApiKeyStatusUpdateRequest;
import org.xhy.gateway.interfaces.api.request.api_key.ApiKeyUpdateRequest;

import java.util.List;

/**
 * API Key 管理控制器 - 内部管理接口
 * 提供API Key的生成、查询、更新、删除功能
 * 不需要API Key校验
 * 
 * @author xhy
 * @since 1.0.0
 */
@RestController
@RequestMapping("/admin/apikeys")
public class AdminApiKeyController {

    private static final Logger logger = LoggerFactory.getLogger(AdminApiKeyController.class);

    private final ApiKeyAppService apiKeyAppService;

    public AdminApiKeyController(ApiKeyAppService apiKeyAppService) {
        this.apiKeyAppService = apiKeyAppService;
    }

    /**
     * 生成 API Key
     */
    @PostMapping
    public Result<ApiKeyDTO> generateApiKey(@RequestBody ApiKeyCreateRequest request) {
        logger.info("接收生成 API Key 请求，描述: {}", request.getDescription());
        
        ApiKeyDTO apiKey = apiKeyAppService.generateApiKey(request.getDescription(), request.getExpiresAt());
        return Result.success("API Key 生成成功", apiKey);
    }

    /**
     * 根据 ID 获取 API Key
     */
    @GetMapping("/{id}")
    public Result<ApiKeyDTO> getById(@PathVariable String id) {
        logger.info("接收获取 API Key 请求，ID: {}", id);
        
        ApiKeyDTO apiKey = apiKeyAppService.getById(id);
        return Result.success(apiKey);
    }

    /**
     * 获取所有 API Key 列表
     */
    @GetMapping
    public Result<List<ApiKeyDTO>> findAll() {
        logger.info("接收获取 API Key 列表请求");
        
        List<ApiKeyDTO> apiKeys = apiKeyAppService.findAll();
        return Result.success(apiKeys);
    }

    /**
     * 修改 API Key
     */
    @PutMapping("/{id}")
    public Result<Void> updateApiKey(@PathVariable String id, @RequestBody ApiKeyUpdateRequest request) {
        logger.info("接收修改 API Key 请求，ID: {}", id);
        
        boolean success = apiKeyAppService.updateApiKey(id, request.getDescription(), request.getExpiresAt());
        if (success) {
            return Result.<Void>success("API Key 修改成功", null);
        } else {
            return Result.badRequest("API Key 修改失败");
        }
    }

    /**
     * 修改 API Key 状态
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable String id, @RequestBody ApiKeyStatusUpdateRequest request) {
        logger.info("接收修改 API Key 状态请求，ID: {}，状态: {}", id, request.getStatus());
        
        ApiKeyStatus status = ApiKeyStatus.valueOf(request.getStatus());
        boolean success = apiKeyAppService.updateStatus(id, status);
        if (success) {
            return Result.<Void>success("API Key 状态修改成功", null);
        } else {
            return Result.badRequest("API Key 状态修改失败");
        }
    }

    /**
     * 删除 API Key
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(@PathVariable String id) {
        logger.info("接收删除 API Key 请求，ID: {}", id);
        
        boolean success = apiKeyAppService.deleteById(id);
        if (success) {
            return Result.<Void>success("API Key 删除成功", null);
        } else {
            return Result.badRequest("API Key 删除失败");
        }
    }

    /**
     * 检查 API Key 是否可用
     */
    @GetMapping("/validate")
    public Result<Boolean> isUsable(@RequestParam String apiKey) {
        logger.info("接收验证 API Key 请求");
        
        boolean usable = apiKeyAppService.isUsable(apiKey);
        return Result.success(usable);
    }
} 