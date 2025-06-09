package org.xhy.gateway.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.gateway.application.assembler.ApiInstanceAssembler;
import org.xhy.gateway.application.dto.ApiInstanceDTO;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceStatus;
import org.xhy.gateway.domain.apiinstance.entity.ApiType;
import org.xhy.gateway.domain.apiinstance.service.ApiInstanceDomainService;
import org.xhy.gateway.domain.project.service.ProjectDomainService;
import org.xhy.gateway.interfaces.api.request.api_instance.ApiInstanceCreateRequest;
import org.xhy.gateway.interfaces.api.request.api_instance.ApiInstanceUpdateRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * API实例应用服务
 * 协调领域服务，处理事务，转换DTO
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class ApiInstanceAppService {

    private static final Logger logger = LoggerFactory.getLogger(ApiInstanceAppService.class);

    private final ApiInstanceDomainService apiInstanceDomainService;

    private final ProjectDomainService projectDomainService;

    public ApiInstanceAppService(ApiInstanceDomainService apiInstanceDomainService, ProjectDomainService projectDomainService) {
        this.apiInstanceDomainService = apiInstanceDomainService;
        this.projectDomainService = projectDomainService;
    }

    /**
     * 检查API实例是否已存在
     */
    public boolean isApiInstanceExists(String projectId, ApiType apiType, String businessId) {
        return apiInstanceDomainService.isApiInstanceExists(projectId, apiType, businessId);
    }

    /**
     * 创建API实例
     */
    @Transactional
    public ApiInstanceDTO createApiInstance(ApiInstanceCreateRequest request, String projectId) {
        projectDomainService.validateProjectExists(projectId);

        // 通过Assembler将请求转换为实体，使用上下文中的projectId
        ApiInstanceEntity entity = ApiInstanceAssembler.toEntity(request, projectId);

        // 先检查是否已存在
        boolean alreadyExists = apiInstanceDomainService.isApiInstanceExists(
            projectId, entity.getApiType(), entity.getBusinessId());

        // 调用领域服务创建
        ApiInstanceEntity createdEntity = apiInstanceDomainService.createApiInstance(entity);

        // 记录日志
        if (alreadyExists) {
            logger.info("API实例已存在，返回已存在实例，实例ID: {}", createdEntity.getId());
        } else {
            logger.info("API实例创建成功，实例ID: {}", createdEntity.getId());
        }

        // 转换为DTO返回
        return ApiInstanceAssembler.toDTO(createdEntity);
    }

    /**
     * 批量创建API实例
     */
    @Transactional
    public List<ApiInstanceDTO> batchCreateApiInstances(List<ApiInstanceCreateRequest> requests, String projectId) {
        if (requests == null || requests.isEmpty()) {
            logger.warn("批量创建API实例失败：请求列表为空");
            return new ArrayList<>();
        }

        logger.info("开始批量创建API实例，数量: {}", requests.size());

        projectDomainService.validateProjectExists(projectId);

        // 通过Assembler将请求列表转换为实体列表，使用上下文中的projectId
        List<ApiInstanceEntity> entities = ApiInstanceAssembler.toEntityList(requests, projectId);

        // 调用领域服务批量创建
        List<ApiInstanceEntity> createdEntities = apiInstanceDomainService.batchCreateApiInstances(entities);

        // 转换为DTO列表返回
        List<ApiInstanceDTO> result = ApiInstanceAssembler.toDTOList(createdEntities);
        
        logger.info("批量创建API实例完成，实际创建数量: {}，跳过重复数量: {}", 
            result.size(), requests.size() - result.size());
        return result;
    }

    /**
     * 根据ID获取API实例详情
     */
    public ApiInstanceDTO getApiInstanceById(String id) {
        ApiInstanceEntity entity = apiInstanceDomainService.getApiInstanceById(id);
        return ApiInstanceAssembler.toDTO(entity);
    }

    /**
     * 根据项目ID获取API实例列表
     */
    public List<ApiInstanceDTO> getApiInstancesByProjectId(String projectId) {
        List<ApiInstanceEntity> entities = apiInstanceDomainService.getApiInstancesByProjectId(projectId);
        return ApiInstanceAssembler.toDTOList(entities);
    }

    /**
     * 根据业务ID和项目ID获取API实例
     */
    public ApiInstanceDTO getApiInstanceByBusinessId(String projectId, String businessId) {
        ApiInstanceEntity entity = apiInstanceDomainService.getApiInstanceByBusinessId(projectId, businessId);
        return ApiInstanceAssembler.toDTO(entity);
    }

    /**
     * 获取指定状态的API实例列表
     */
    public List<ApiInstanceDTO> getApiInstancesByStatus(ApiInstanceStatus status) {
        List<ApiInstanceEntity> entities = apiInstanceDomainService.getApiInstancesByStatus(status);
        return ApiInstanceAssembler.toDTOList(entities);
    }

    /**
     * 根据API类型获取API实例列表
     */
    public List<ApiInstanceDTO> getApiInstancesByApiType(String projectId, ApiType apiType) {
        List<ApiInstanceEntity> entities = apiInstanceDomainService.getApiInstancesByApiType(projectId, apiType);
        return ApiInstanceAssembler.toDTOList(entities);
    }

    /**
     * 更新API实例
     */
    @Transactional
    public ApiInstanceDTO updateApiInstance(String projectId, String apiType, String businessId, ApiInstanceUpdateRequest request) {
        logger.info("开始更新API实例，项目ID: {}，API类型: {}，业务ID: {}", projectId, apiType, businessId);

        projectDomainService.validateProjectExists(projectId);

        // 根据projectId、apiType、businessId查找现有实例
        ApiInstanceEntity existingEntity = apiInstanceDomainService.getApiInstanceByBusinessKey(projectId, ApiType.fromCode(apiType), businessId);
        
        // 通过Assembler将请求转换为实体，并设置正确的标识信息
        ApiInstanceEntity updateEntity = ApiInstanceAssembler.toEntity(request, projectId);
        updateEntity.setId(existingEntity.getId()); // 保持原有ID
        updateEntity.setApiType(ApiType.fromCode(apiType)); // 确保API类型正确
        updateEntity.setBusinessId(businessId); // 确保业务ID正确
        
        // 调用领域服务更新
        ApiInstanceEntity updatedEntity = apiInstanceDomainService.updateApiInstance(updateEntity);
        
        return ApiInstanceAssembler.toDTO(updatedEntity);
    }

    // 根据 project，业务 id，类型删除
    public void deleteApiInstance(String projectId, String businessId, ApiType apiType) {
        apiInstanceDomainService.deleteApiInstance(projectId, businessId, apiType);
    }

    /**
     * 激活API实例
     */
    @Transactional
    public ApiInstanceDTO activateApiInstance(String projectId, String apiType, String businessId) {
        logger.info("激活API实例，项目ID: {}, API类型: {}, 业务ID: {}", projectId, apiType, businessId);
        
        ApiInstanceEntity entity = apiInstanceDomainService.getApiInstanceByBusinessKey(projectId, ApiType.fromCode(apiType), businessId);
        entity.activate();
        
        // 调用领域服务更新
        ApiInstanceEntity updatedEntity = apiInstanceDomainService.updateApiInstance(entity);
        
        return ApiInstanceAssembler.toDTO(updatedEntity);
    }

    /**
     * 停用API实例
     */
    @Transactional
    public ApiInstanceDTO deactivateApiInstance(String projectId, String apiType, String businessId) {
        logger.info("停用API实例，项目ID: {}, API类型: {}, 业务ID: {}", projectId, apiType, businessId);
        
        ApiInstanceEntity entity = apiInstanceDomainService.getApiInstanceByBusinessKey(projectId, ApiType.fromCode(apiType), businessId);
        entity.deactivate();
        
        // 调用领域服务更新
        ApiInstanceEntity updatedEntity = apiInstanceDomainService.updateApiInstance(entity);
        
        return ApiInstanceAssembler.toDTO(updatedEntity);
    }

    /**
     * 标记API实例为已弃用
     */
    @Transactional
    public ApiInstanceDTO deprecateApiInstance(String projectId, String apiType, String businessId) {
        logger.info("标记API实例为已弃用，项目ID: {}, API类型: {}, 业务ID: {}", projectId, apiType, businessId);
        
        ApiInstanceEntity entity = apiInstanceDomainService.getApiInstanceByBusinessKey(projectId, ApiType.fromCode(apiType), businessId);
        entity.deprecate();
        
        // 调用领域服务更新
        ApiInstanceEntity updatedEntity = apiInstanceDomainService.updateApiInstance(entity);
        
        return ApiInstanceAssembler.toDTO(updatedEntity);
    }

    /**
     * 获取所有API实例（包含项目信息）- 用于管理后台
     */
    @Transactional(readOnly = true)
    public List<ApiInstanceDTO> getAllInstancesWithProjects(String projectId, ApiInstanceStatus status) {
        logger.info("获取所有API实例，项目ID: {}，状态: {}", projectId, status);
        
        List<ApiInstanceEntity> entities = apiInstanceDomainService.getAllInstancesWithProjects(projectId, status);
        List<ApiInstanceDTO> dtos = ApiInstanceAssembler.toDTOList(entities);
        
        // 填充项目名称
        for (ApiInstanceDTO dto : dtos) {
            // 获取项目名称
            try {
                String pName = projectDomainService.getProjectNameById(dto.getProjectId());
                dto.setProjectName(pName);
            } catch (Exception e) {
                logger.warn("获取项目名称失败，项目ID: {}", dto.getProjectId());
                dto.setProjectName("未知项目");
            }
        }
        
        return dtos;
    }
} 