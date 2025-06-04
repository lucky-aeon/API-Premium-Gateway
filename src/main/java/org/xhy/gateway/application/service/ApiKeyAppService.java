package org.xhy.gateway.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.gateway.application.assembler.ApiKeyAssembler;
import org.xhy.gateway.application.dto.ApiKeyDTO;
import org.xhy.gateway.domain.apikey.entity.ApiKeyEntity;
import org.xhy.gateway.domain.apikey.entity.ApiKeyStatus;
import org.xhy.gateway.domain.apikey.service.ApiKeyDomainService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key 应用服务
 * 协调领域服务，处理事务
 */
@Service
public class ApiKeyAppService {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyAppService.class);

    private final ApiKeyDomainService apiKeyDomainService;
    private final ApiKeyAssembler apiKeyAssembler;

    public ApiKeyAppService(ApiKeyDomainService apiKeyDomainService, ApiKeyAssembler apiKeyAssembler) {
        this.apiKeyDomainService = apiKeyDomainService;
        this.apiKeyAssembler = apiKeyAssembler;
    }

    /**
     * 生成 API Key
     */
    @Transactional(rollbackFor = Exception.class)
    public ApiKeyDTO generateApiKey(String description, LocalDateTime expiresAt) {
        logger.info("应用服务：生成 API Key，描述: {}", description);
        
        ApiKeyEntity entity = apiKeyDomainService.generateApiKey(description, expiresAt);
        return apiKeyAssembler.toDTO(entity);
    }

    /**
     * 根据 ID 获取 API Key
     */
    @Transactional(readOnly = true)
    public ApiKeyDTO getById(String id) {
        ApiKeyEntity entity = apiKeyDomainService.getById(id);
        return apiKeyAssembler.toDTO(entity);
    }

    /**
     * 获取所有 API Key 列表
     */
    @Transactional(readOnly = true)
    public List<ApiKeyDTO> findAll() {
        List<ApiKeyEntity> entities = apiKeyDomainService.findAll();
        return apiKeyAssembler.toDTOList(entities);
    }

    /**
     * 修改 API Key
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateApiKey(String id, String description, LocalDateTime expiresAt) {
        logger.info("应用服务：修改 API Key，ID: {}", id);
        return apiKeyDomainService.updateApiKey(id, description, expiresAt);
    }

    /**
     * 修改 API Key 状态
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean updateStatus(String id, ApiKeyStatus status) {
        logger.info("应用服务：修改 API Key 状态，ID: {}，状态: {}", id, status);
        return apiKeyDomainService.updateStatus(id, status);
    }

    /**
     * 删除 API Key
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteById(String id) {
        logger.info("应用服务：删除 API Key，ID: {}", id);
        return apiKeyDomainService.deleteById(id);
    }

    /**
     * 检查 API Key 是否可用
     */
    @Transactional(readOnly = true)
    public boolean isUsable(String apiKeyValue) {
        return apiKeyDomainService.isUsable(apiKeyValue);
    }
} 