package org.xhy.gateway.application.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.xhy.gateway.application.assembler.ProjectAssembler;
import org.xhy.gateway.application.dto.ProjectDTO;
import org.xhy.gateway.application.dto.ProjectSimpleDTO;
import org.xhy.gateway.domain.project.entity.ProjectEntity;
import org.xhy.gateway.domain.project.service.ProjectDomainService;
import org.xhy.gateway.interfaces.api.request.ProjectCreateRequest;

import java.util.List;

/**
 * 项目应用服务
 * 负责项目相关的领域编排和事务管理
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class ProjectAppService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectAppService.class);

    private final ProjectDomainService projectDomainService;
    private final ProjectAssembler projectAssembler;

    public ProjectAppService(ProjectDomainService projectDomainService, ProjectAssembler projectAssembler) {
        this.projectDomainService = projectDomainService;
        this.projectAssembler = projectAssembler;
    }

    /**
     * 创建项目
     * 需要事务支持，因为涉及到数据更新
     */
    @Transactional(rollbackFor = Exception.class)
    public ProjectDTO createProject(ProjectCreateRequest request) {
        logger.info("应用层开始创建项目，请求: {}", request);

        // 调用领域服务创建项目
        ProjectEntity projectEntity = projectDomainService.createProject(
                request.getName(),
                request.getDescription(),
                request.getApiKey()
        );

        // 转换为DTO返回
        ProjectDTO result = projectAssembler.toDTO(projectEntity);
        
        logger.info("应用层项目创建成功，项目ID: {}", result.getId());
        return result;
    }

    /**
     * 根据ID获取项目详情
     * 只读操作，不需要事务
     */
    public ProjectDTO getProjectById(String id) {
        logger.debug("应用层获取项目详情，项目ID: {}", id);

        // 调用领域服务获取项目
        ProjectEntity projectEntity = projectDomainService.getProjectById(id);

        // 转换为DTO返回
        return projectAssembler.toDTO(projectEntity);
    }

    /**
     * 获取项目列表
     * 只读操作，不需要事务
     */
    public List<ProjectDTO> getProjectList() {
        logger.debug("应用层获取项目列表");

        // 调用领域服务获取项目列表
        List<ProjectEntity> projectEntities = projectDomainService.getProjectList();

        // 转换为DTO列表返回
        return projectAssembler.toDTOList(projectEntities);
    }

    /**
     * 获取所有项目（管理后台用）
     * 只读操作，不需要事务
     */
    public List<ProjectDTO> getAllProjects() {
        logger.debug("应用层获取所有项目列表（管理后台）");

        // 调用领域服务获取所有项目
        List<ProjectEntity> projectEntities = projectDomainService.getAllProjects();

        // 转换为DTO列表返回
        return projectAssembler.toDTOList(projectEntities);
    }

    /**
     * 根据项目名称搜索项目
     * 只读操作，不需要事务
     */
    public List<ProjectDTO> searchProjectsByName(String projectName) {
        logger.debug("应用层按名称搜索项目，项目名称: {}", projectName);

        // 调用领域服务搜索项目
        List<ProjectEntity> projectEntities = projectDomainService.searchProjectsByName(projectName);

        // 转换为DTO列表返回
        return projectAssembler.toDTOList(projectEntities);
    }

    /**
     * 根据状态获取项目列表
     * 只读操作，不需要事务
     */
    public List<ProjectDTO> getProjectsByStatus(String status) {
        logger.debug("应用层按状态查询项目，状态: {}", status);

        // 调用领域服务获取项目
        List<ProjectEntity> projectEntities = projectDomainService.getProjectsByStatus(status);

        // 转换为DTO列表返回
        return projectAssembler.toDTOList(projectEntities);
    }

    /**
     * 删除项目（管理员权限）
     * 需要事务支持，会级联删除相关数据
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(String projectId) {
        logger.warn("应用层删除项目，项目ID: {}", projectId);

        // 调用领域服务删除项目（会级联删除相关的实例和指标数据）
        projectDomainService.deleteProject(projectId);

        logger.warn("应用层项目删除完成，项目ID: {}", projectId);
    }

    /**
     * 获取项目统计信息
     * 只读操作，不需要事务
     */
    public Object getProjectStatistics() {
        logger.debug("应用层获取项目统计信息");

        // TODO: 调用领域服务获取统计信息
        return projectDomainService.getProjectStatistics();
    }

    /**
     * 获取简化项目列表（仅ID和名称）
     * 用于下拉选择器等场景
     */
    public List<ProjectSimpleDTO> getSimpleProjectList() {
        logger.debug("应用层获取简化项目列表");

        // 调用领域服务获取所有项目
        List<ProjectEntity> projectEntities = projectDomainService.getAllProjects();

        // 转换为简化DTO列表返回
        return projectAssembler.toSimpleDTOList(projectEntities);
    }
} 