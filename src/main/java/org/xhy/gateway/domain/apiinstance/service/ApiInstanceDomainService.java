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
import org.xhy.gateway.infrastructure.exception.BusinessException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
     * 需要验证项目是否存在且激活，如果已存在则返回已存在的实例
     */
    public ApiInstanceEntity createApiInstance(ApiInstanceEntity apiInstanceEntity) {
        // 检查是否已存在
        if (isApiInstanceExists(apiInstanceEntity.getProjectId(), 
                               apiInstanceEntity.getApiType(), 
                               apiInstanceEntity.getBusinessId())) {
            logger.info("API实例已存在，返回已存在的实例：projectId={}, apiType={}, businessId={}", 
                apiInstanceEntity.getProjectId(), apiInstanceEntity.getApiType(), apiInstanceEntity.getBusinessId());
            
            // 返回已存在的实例
            return getApiInstanceByProjectIdAndBusinessId(apiInstanceEntity.getProjectId(), 
                                                          apiInstanceEntity.getApiType(),
                                                          apiInstanceEntity.getBusinessId());
        }

        apiInstanceRepository.insert(apiInstanceEntity);
        logger.info("API实例创建成功，实例ID: {}，业务ID: {}", apiInstanceEntity.getId(), apiInstanceEntity.getBusinessId());
        return apiInstanceEntity;
    }

    /**
     * 根据项目ID、API类型和业务ID获取API实例
     */
    private ApiInstanceEntity getApiInstanceByProjectIdAndBusinessId(String projectId, ApiType apiType, String businessId) {
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiInstanceEntity::getProjectId, projectId)
                   .eq(ApiInstanceEntity::getApiType, apiType)
                   .eq(ApiInstanceEntity::getBusinessId, businessId);
        
        ApiInstanceEntity existing = apiInstanceRepository.selectOne(queryWrapper);
        if (existing == null) {
            throw new EntityNotFoundException("API实例不存在，projectId=" + projectId 
                + ", apiType=" + apiType + ", businessId=" + businessId);
        }
        
        return existing;
    }

    /**
     * 批量创建API实例
     * 提高创建多个实例时的性能
     */
    public List<ApiInstanceEntity> batchCreateApiInstances(List<ApiInstanceEntity> apiInstanceEntities) {
        if (apiInstanceEntities == null || apiInstanceEntities.isEmpty()) {
            logger.warn("批量创建API实例失败：实例列表为空");
            return new ArrayList<>();
        }

        logger.info("开始批量创建API实例，数量: {}", apiInstanceEntities.size());

        // 批量查询已存在的实例
        List<ApiInstanceEntity> existingInstances = batchQueryExistingInstances(apiInstanceEntities);
        
        // 构建已存在实例的唯一标识集合，用于快速查找
        Set<String> existingKeys = existingInstances.stream()
                .map(instance -> buildUniqueKey(instance.getProjectId(), instance.getApiType(), instance.getBusinessId()))
                .collect(Collectors.toSet());

        // 过滤掉已存在的实例
        List<ApiInstanceEntity> newInstances = apiInstanceEntities.stream()
                .filter(entity -> {
                    String key = buildUniqueKey(entity.getProjectId(), entity.getApiType(), entity.getBusinessId());
                    boolean exists = existingKeys.contains(key);
                    if (exists) {
                        logger.info("API实例已存在，跳过创建：projectId={}, apiType={}, businessId={}", 
                            entity.getProjectId(), entity.getApiType(), entity.getBusinessId());
                    }
                    return !exists;
                })
                .collect(Collectors.toList());

        if (newInstances.isEmpty()) {
            logger.info("所有API实例都已存在，无需创建");
            return new ArrayList<>();
        }

        // 批量插入新实例
        for (ApiInstanceEntity entity : newInstances) {
            apiInstanceRepository.insert(entity);
        }

        logger.info("批量创建API实例成功，成功创建数量: {}，跳过重复数量: {}", 
            newInstances.size(), apiInstanceEntities.size() - newInstances.size());
        return newInstances;
    }

    /**
     * 批量查询已存在的实例
     */
    private List<ApiInstanceEntity> batchQueryExistingInstances(List<ApiInstanceEntity> apiInstanceEntities) {
        if (apiInstanceEntities.isEmpty()) {
            return new ArrayList<>();
        }

        // 收集所有需要检查的条件和待检查的唯一key集合
        Set<String> projectIds = new HashSet<>();
        Set<ApiType> apiTypes = new HashSet<>();
        Set<String> businessIds = new HashSet<>();
        Set<String> keysToCheck = new HashSet<>();

        for (ApiInstanceEntity entity : apiInstanceEntities) {
            projectIds.add(entity.getProjectId());
            apiTypes.add(entity.getApiType());
            businessIds.add(entity.getBusinessId());
            keysToCheck.add(buildUniqueKey(entity.getProjectId(), entity.getApiType(), entity.getBusinessId()));
        }

        // 构建查询条件：查询所有可能重复的记录
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(ApiInstanceEntity::getProjectId, projectIds)
                   .in(ApiInstanceEntity::getApiType, apiTypes)
                   .in(ApiInstanceEntity::getBusinessId, businessIds);

        List<ApiInstanceEntity> allCandidates = apiInstanceRepository.selectList(queryWrapper);

        // 使用Set提高匹配效率：只返回真正匹配的记录
        return allCandidates.stream()
                .filter(existing -> {
                    String existingKey = buildUniqueKey(existing.getProjectId(), existing.getApiType(), existing.getBusinessId());
                    return keysToCheck.contains(existingKey);
                })
                .collect(Collectors.toList());
    }

    /**
     * 构建唯一标识Key
     */
    private String buildUniqueKey(String projectId, ApiType apiType, String businessId) {
        return projectId + ":" + apiType.name() + ":" + businessId;
    }

    /**
     * 检查API实例是否已存在
     */
    public boolean isApiInstanceExists(String projectId, ApiType apiType, String businessId) {
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiInstanceEntity::getProjectId, projectId)
                   .eq(ApiInstanceEntity::getApiType, apiType)
                   .eq(ApiInstanceEntity::getBusinessId, businessId);
        
        return apiInstanceRepository.selectCount(queryWrapper) > 0;
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

    /**
     * 根据业务键获取API实例（projectId + apiType + businessId）
     */
    public ApiInstanceEntity getApiInstanceByBusinessKey(String projectId, ApiType apiType, String businessId) {
        logger.debug("根据业务键获取API实例，项目ID: {}，API类型: {}，业务ID: {}", projectId, apiType, businessId);
        
        LambdaQueryWrapper<ApiInstanceEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiInstanceEntity::getProjectId, projectId)
                   .eq(ApiInstanceEntity::getApiType, apiType)
                   .eq(ApiInstanceEntity::getBusinessId, businessId);
        
        ApiInstanceEntity apiInstance = apiInstanceRepository.selectOne(queryWrapper);
        if (apiInstance == null) {
            throw new EntityNotFoundException("API实例不存在，projectId=" + projectId 
                + ", apiType=" + apiType + ", businessId=" + businessId);
        }
        
        return apiInstance;
    }
} 