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
     * 创建API实例
     */
    @Transactional
    public ApiInstanceDTO createApiInstance(ApiInstanceCreateRequest request) {
        logger.info("开始创建API实例，项目ID: {}，业务ID: {}", request.getProjectId(), request.getBusinessId());

        projectDomainService.validateProjectExists(request.getProjectId());

        // 通过Assembler将请求转换为实体
        ApiInstanceEntity entity = ApiInstanceAssembler.toEntity(request);

        // 调用领域服务创建
        ApiInstanceEntity createdEntity = apiInstanceDomainService.createApiInstance(entity);

        // 转换为DTO返回
        return ApiInstanceAssembler.toDTO(createdEntity);
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
    public ApiInstanceDTO updateApiInstance(String id, ApiInstanceUpdateRequest request) {
        logger.info("开始更新API实例，实例ID: {}", id);

        projectDomainService.validateProjectExists(request.getProjectId());

        ApiInstanceEntity entity = ApiInstanceAssembler.toEntity(request);
        // 调用领域服务更新
        apiInstanceDomainService.updateApiInstance(entity);
        return ApiInstanceAssembler.toDTO(entity);
    }

    // 根据 project，业务 id，类型删除
    public void deleteApiInstance(String projectId, String businessId, ApiType apiType) {
        apiInstanceDomainService.deleteApiInstance(projectId, businessId, apiType);
    }

    /**
     * 激活API实例
     */
    @Transactional
    public ApiInstanceDTO activateApiInstance(String id) {
        logger.info("激活API实例，实例ID: {}", id);
        
        ApiInstanceEntity entity = apiInstanceDomainService.getApiInstanceById(id);
        entity.activate();
        
        // 调用领域服务更新
        ApiInstanceEntity updatedEntity = apiInstanceDomainService.updateApiInstance(entity);
        
        return ApiInstanceAssembler.toDTO(updatedEntity);
    }

    /**
     * 停用API实例
     */
    @Transactional
    public ApiInstanceDTO deactivateApiInstance(String id) {
        logger.info("停用API实例，实例ID: {}", id);
        
        ApiInstanceEntity entity = apiInstanceDomainService.getApiInstanceById(id);
        entity.deactivate();
        
        // 调用领域服务更新
        ApiInstanceEntity updatedEntity = apiInstanceDomainService.updateApiInstance(entity);
        
        return ApiInstanceAssembler.toDTO(updatedEntity);
    }

    /**
     * 标记API实例为已弃用
     */
    @Transactional
    public ApiInstanceDTO deprecateApiInstance(String id) {
        logger.info("标记API实例为已弃用，实例ID: {}", id);
        
        ApiInstanceEntity entity = apiInstanceDomainService.getApiInstanceById(id);
        entity.deprecate();
        
        // 调用领域服务更新
        ApiInstanceEntity updatedEntity = apiInstanceDomainService.updateApiInstance(entity);
        
        return ApiInstanceAssembler.toDTO(updatedEntity);
    }
} 