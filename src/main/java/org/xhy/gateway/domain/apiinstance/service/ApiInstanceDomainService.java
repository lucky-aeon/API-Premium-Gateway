package org.xhy.gateway.domain.apiinstance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.apiinstance.repository.ApiInstanceRepository;
import org.xhy.gateway.infrastructure.exception.EntityNotFoundException;

import java.util.List;

/**
 * API实例领域服务
 * 处理API实例相关的复杂业务逻辑
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class ApiInstanceDomainService {

    private static final Logger logger = LoggerFactory.getLogger(ApiInstanceDomainService.class);

    private final ApiInstanceRepository apiInstanceRepository;

    public ApiInstanceDomainService(ApiInstanceRepository apiInstanceRepository) {
        this.apiInstanceRepository = apiInstanceRepository;
    }

    /**
     * 创建API实例
     * 需要验证项目是否存在且激活
     */
    public ApiInstanceEntity createApiInstance(ApiInstanceEntity apiInstanceEntity) {
        apiInstanceRepository.insert(apiInstanceEntity);
        logger.info("API实例创建成功，实例ID: {}，业务ID: {}", apiInstanceEntity.getId(), apiInstanceEntity.getBusinessId());
        return apiInstanceEntity;
    }

    /**
     * 根据ID获取API实例详情
     */
    public ApiInstanceEntity getApiInstanceById(String id) {
        logger.debug("获取API实例详情，实例ID: {}", id);
        
        ApiInstanceEntity apiInstance = apiInstanceRepository.selectById(id);
        if (apiInstance == null) {
            throw new EntityNotFoundException("API实例不存在，ID: " + id);
        }
        
        return apiInstance;
    }

    /**
     * 根据项目ID获取API实例列表
     */
    public List<ApiInstanceEntity> getApiInstancesByProjectId(String projectId) {
        logger.debug("获取项目的API实例列表，项目ID: {}", projectId);
        
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiInstanceEntity::getProjectId, projectId);
        queryWrapper.orderByDesc(ApiInstanceEntity::getCreatedAt);
        
        return apiInstanceRepository.selectList(queryWrapper);
    }

    /**
     * 根据业务ID和项目ID获取API实例
     */
    public ApiInstanceEntity getApiInstanceByBusinessId(String projectId, String businessId) {
        logger.debug("根据业务ID获取API实例，项目ID: {}，业务ID: {}", projectId, businessId);
        
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiInstanceEntity::getProjectId, projectId);
        queryWrapper.eq(ApiInstanceEntity::getBusinessId, businessId);
        
        ApiInstanceEntity apiInstance = apiInstanceRepository.selectOne(queryWrapper);
        if (apiInstance == null) {
            throw new EntityNotFoundException("API实例不存在，项目ID: " + projectId + ", 业务ID: " + businessId);
        }
        
        return apiInstance;
    }

    /**
     * 获取指定状态的API实例列表
     */
    public List<ApiInstanceEntity> getApiInstancesByStatus(ApiInstanceStatus status) {
        logger.debug("获取指定状态的API实例列表，状态: {}", status);
        
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiInstanceEntity::getStatus, status);
        queryWrapper.orderByDesc(ApiInstanceEntity::getCreatedAt);
        
        return apiInstanceRepository.selectList(queryWrapper);
    }

    /**
     * 根据API类型获取API实例列表
     */
    public List<ApiInstanceEntity> getApiInstancesByApiType(String projectId, ApiType apiType) {
        logger.debug("根据API类型获取实例列表，项目ID: {}，API类型: {}", projectId, apiType);
        
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiInstanceEntity::getProjectId, projectId);
        queryWrapper.eq(ApiInstanceEntity::getApiType, apiType);
        queryWrapper.orderByDesc(ApiInstanceEntity::getCreatedAt);
        
        return apiInstanceRepository.selectList(queryWrapper);
    }

    /**
     * 更新API实例
     */
    public ApiInstanceEntity updateApiInstance(ApiInstanceEntity apiInstanceEntity) {
        logger.info("更新API实例，实例ID: {}", apiInstanceEntity.getId());
        
        int updatedRows = apiInstanceRepository.updateById(apiInstanceEntity);
        if (updatedRows == 0) {
            throw new EntityNotFoundException("API实例更新失败，实例不存在，ID: " + apiInstanceEntity.getId());
        }
        
        logger.info("API实例更新成功，实例ID: {}", apiInstanceEntity.getId());
        return apiInstanceEntity;
    }

    /**
     * 删除API实例
     */
    public boolean deleteApiInstance(String id) {
        logger.info("删除API实例，实例ID: {}", id);
        
        int deletedRows = apiInstanceRepository.deleteById(id);
        boolean success = deletedRows > 0;
        
        if (success) {
            logger.info("API实例删除成功，实例ID: {}", id);
        }
        
        return success;
    }

    /**
     * 获取所有API实例（用于管理后台）
     */
    public List<ApiInstanceEntity> getAllInstancesWithProjects(String projectId, ApiInstanceStatus status) {
        logger.debug("获取所有API实例，项目ID: {}，状态: {}", projectId, status);
        
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        
        // 按项目ID过滤
        if (projectId != null && !projectId.trim().isEmpty()) {
            queryWrapper.eq(ApiInstanceEntity::getProjectId, projectId);
        }
        
        // 按状态过滤
        if (status != null) {
            queryWrapper.eq(ApiInstanceEntity::getStatus, status);
        }
        
        queryWrapper.orderByDesc(ApiInstanceEntity::getCreatedAt);
        
        return apiInstanceRepository.selectList(queryWrapper);
    }

    public void deleteApiInstance(String projectId, String businessId, ApiType apiType) {
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiInstanceEntity::getProjectId, projectId);
        queryWrapper.eq(ApiInstanceEntity::getBusinessId, businessId);
        queryWrapper.eq(ApiInstanceEntity::getApiType, apiType);

        apiInstanceRepository.delete(queryWrapper);
    }
} 