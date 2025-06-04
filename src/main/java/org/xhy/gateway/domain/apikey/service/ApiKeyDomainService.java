package org.xhy.gateway.domain.apikey.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.gateway.domain.apikey.entity.ApiKeyEntity;
import org.xhy.gateway.domain.apikey.entity.ApiKeyStatus;
import org.xhy.gateway.domain.apikey.repository.ApiKeyRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

/**
 * API Key 领域服务
 * 处理 API Key 相关的复杂业务逻辑
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class ApiKeyDomainService {

    private static final Logger logger = LoggerFactory.getLogger(ApiKeyDomainService.class);

    private final ApiKeyRepository apiKeyRepository;
    private final SecureRandom secureRandom;

    // API Key 配置
    private static final String API_KEY_PREFIX = "gw_";
    private static final int API_KEY_LENGTH = 32;
    private static final String API_KEY_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public ApiKeyDomainService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.secureRandom = new SecureRandom();
    }

    /**
     * 生成 apiKey
     */
    public ApiKeyEntity generateApiKey(String description, LocalDateTime expiresAt) {
        logger.info("生成 API Key，描述: {}，过期时间: {}", description, expiresAt);
        
        String apiKeyValue = generateUniqueApiKeyValue();
        ApiKeyEntity apiKey = new ApiKeyEntity(apiKeyValue, description, expiresAt);
        
        apiKeyRepository.insert(apiKey);
        logger.info("成功生成 API Key，ID: {}", apiKey.getId());
        return apiKey;
    }

    /**
     * 根据 id 获取 apiKey
     */
    public ApiKeyEntity getById(String id) {
        return apiKeyRepository.selectById(id);
    }

    /**
     * 获取 apiKey 列表
     */
    public List<ApiKeyEntity> findAll() {
        LambdaQueryWrapper<ApiKeyEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ApiKeyEntity::getCreatedAt);
        return apiKeyRepository.selectList(queryWrapper);
    }

    /**
     * 修改 apiKey，只能修改描述和过期时间
     */
    public boolean updateApiKey(String id, String description, LocalDateTime expiresAt) {
        LambdaUpdateWrapper<ApiKeyEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ApiKeyEntity::getId, id);
        
        if (description != null) {
            updateWrapper.set(ApiKeyEntity::getDescription, description);
        }
        if (expiresAt != null) {
            updateWrapper.set(ApiKeyEntity::getExpiresAt, expiresAt);
        }
        
        // 如果没有任何字段需要更新，直接返回false
        if (description == null && expiresAt == null) {
            return false;
        }
        
        int updatedRows = apiKeyRepository.update(null, updateWrapper);
        boolean success = updatedRows > 0;
        
        if (success) {
            logger.info("修改 API Key 成功，ID: {}", id);
        }
        
        return success;
    }

    /**
     * 修改 apiKey 状态
     */
    public boolean updateStatus(String id, ApiKeyStatus status) {
        LambdaUpdateWrapper<ApiKeyEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(ApiKeyEntity::getId, id);
        updateWrapper.set(ApiKeyEntity::getStatus, status);
        
        int updatedRows = apiKeyRepository.update(null, updateWrapper);
        boolean success = updatedRows > 0;
        
        if (success) {
            logger.info("修改 API Key 状态成功，ID: {}，新状态: {}", id, status);
        }
        
        return success;
    }

    /**
     * 删除 apiKey
     */
    public boolean deleteById(String id) {
        int deletedRows = apiKeyRepository.deleteById(id);
        boolean success = deletedRows > 0;
        
        if (success) {
            logger.info("删除 API Key 成功，ID: {}", id);
        }
        
        return success;
    }

    /**
     * 根据 apiKey 查看状态是否可用
     */
    public boolean isUsable(String apiKeyValue) {
        LambdaQueryWrapper<ApiKeyEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiKeyEntity::getApiKeyValue, apiKeyValue);
        
        ApiKeyEntity apiKey = apiKeyRepository.selectOne(queryWrapper);
        if (apiKey == null) {
            return false;
        }
        
        return apiKey.isUsable();
    }

    private String generateUniqueApiKeyValue() {
        String apiKeyValue;
        int attempts = 0;
        int maxAttempts = 10;
        
        do {
            apiKeyValue = generateApiKeyValue();
            attempts++;
            
            if (attempts > maxAttempts) {
                logger.error("生成唯一 API Key 值失败，尝试次数超过最大限制: {}", maxAttempts);
                throw new RuntimeException("无法生成唯一的 API Key");
            }
            
        } while (!isApiKeyValueUnique(apiKeyValue));
        
        return apiKeyValue;
    }

    private String generateApiKeyValue() {
        StringBuilder apiKey = new StringBuilder(API_KEY_PREFIX);
        
        for (int i = 0; i < API_KEY_LENGTH; i++) {
            int randomIndex = secureRandom.nextInt(API_KEY_CHARS.length());
            apiKey.append(API_KEY_CHARS.charAt(randomIndex));
        }
        
        return apiKey.toString();
    }

    private boolean isApiKeyValueUnique(String apiKeyValue) {
        LambdaQueryWrapper<ApiKeyEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiKeyEntity::getApiKeyValue, apiKeyValue);
        return apiKeyRepository.selectCount(queryWrapper) == 0;
    }
} 