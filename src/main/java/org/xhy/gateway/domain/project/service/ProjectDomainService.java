package org.xhy.gateway.domain.project.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.xhy.gateway.domain.apikey.repository.ApiKeyRepository;
import org.xhy.gateway.domain.apikey.entity.ApiKeyEntity;
import org.xhy.gateway.domain.project.entity.ProjectEntity;
import org.xhy.gateway.domain.project.repository.ProjectRepository;
import org.xhy.gateway.infrastructure.exception.BusinessException;
import org.xhy.gateway.infrastructure.exception.EntityNotFoundException;

import java.util.List;

/**
 * 项目领域服务
 * 处理项目相关的复杂业务逻辑
 * 
 * @author xhy
 * @since 1.0.0
 */
@Service
public class ProjectDomainService {

    private static final Logger logger = LoggerFactory.getLogger(ProjectDomainService.class);

    private final ProjectRepository projectRepository;
    private final ApiKeyRepository apiKeyRepository;

    public ProjectDomainService(ProjectRepository projectRepository, ApiKeyRepository apiKeyRepository) {
        this.projectRepository = projectRepository;
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * 添加项目
     * 需要验证传入的apiKey是否存在并且激活
     */
    public ProjectEntity createProject(String name, String description, String apiKey) {
        logger.info("开始创建项目，项目名: {}，API Key: {}", name, apiKey);
        
        // 验证apiKey是否存在并且激活
        validateApiKey(apiKey);
        
        // 检查项目名称是否已存在
        validateProjectNameUnique(name);
        
        // 创建项目实体
        ProjectEntity project = new ProjectEntity(name, description, apiKey);
        
        // 保存项目
        projectRepository.insert(project);
        
        logger.info("项目创建成功，项目ID: {}，项目名: {}", project.getId(), project.getName());
        return project;
    }

    /**
     * 根据ID获取项目详情
     */
    public ProjectEntity getProjectById(String id) {
        logger.debug("获取项目详情，项目ID: {}", id);
        
        ProjectEntity project = projectRepository.selectById(id);
        if (project == null) {
            throw new EntityNotFoundException("项目不存在，ID: " + id);
        }
        
        return project;
    }

    /**
     * 获取项目列表
     */
    public List<ProjectEntity> getProjectList() {
        logger.debug("获取项目列表");
        
        LambdaQueryWrapper<ProjectEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ProjectEntity::getCreatedAt);
        
        return projectRepository.selectList(queryWrapper);
    }

    /**
     * 验证API Key是否存在并且可用
     */
    private void validateApiKey(String apiKey) {
        LambdaQueryWrapper<ApiKeyEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ApiKeyEntity::getApiKeyValue, apiKey);
        
        ApiKeyEntity apiKeyEntity = apiKeyRepository.selectOne(queryWrapper);
        if (apiKeyEntity == null) {
            throw new BusinessException("INVALID_API_KEY", "API Key 不存在: " + apiKey);
        }
        
        if (!apiKeyEntity.isUsable()) {
            throw new BusinessException("API_KEY_UNUSABLE", "API Key 不可用，状态: " + apiKeyEntity.getStatus());
        }
        
        logger.debug("API Key 验证通过: {}", apiKey);
    }

    /**
     * 验证项目名称是否唯一
     */
    private void validateProjectNameUnique(String name) {
        LambdaQueryWrapper<ProjectEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectEntity::getName, name);
        
        Long count = projectRepository.selectCount(queryWrapper);
        if (count > 0) {
            throw new BusinessException("DUPLICATE_PROJECT_NAME", "项目名称已存在: " + name);
        }
        
        logger.debug("项目名称唯一性验证通过: {}", name);
    }

    // 是否存在，不存在抛异常
    public void validateProjectExists(String projectId) {
        ProjectEntity project = projectRepository.selectById(projectId);
        if (project == null) {
            throw new EntityNotFoundException("项目不存在，ID: " + projectId);
        }
    }

    /**
     * 检查项目是否存在且处于活跃状态
     * 
     * @param projectId 项目ID
     * @return 如果项目存在且活跃返回true，否则返回false
     */
    public boolean isProjectActive(String projectId) {
        if (projectId == null || projectId.trim().isEmpty()) {
            return false;
        }
        
        ProjectEntity project = projectRepository.selectById(projectId);
        if (project == null) {
            logger.debug("项目不存在: {}", projectId);
            return false;
        }
        
        boolean isActive = project.isActive();
        if (!isActive) {
            logger.debug("项目不是活跃状态: {}, 状态: {}", projectId, project.getStatus());
        }
        
        return isActive;
    }

    /**
     * 获取所有项目（管理后台用）
     */
    public List<ProjectEntity> getAllProjects() {
        logger.debug("获取所有项目列表（管理后台）");
        
        LambdaQueryWrapper<ProjectEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(ProjectEntity::getCreatedAt);
        
        return projectRepository.selectList(queryWrapper);
    }

    /**
     * 根据项目名称搜索项目
     */
    public List<ProjectEntity> searchProjectsByName(String projectName) {
        logger.debug("按名称搜索项目，项目名称: {}", projectName);
        
        LambdaQueryWrapper<ProjectEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(ProjectEntity::getName, projectName)
                   .orderByDesc(ProjectEntity::getCreatedAt);
        
        return projectRepository.selectList(queryWrapper);
    }

    /**
     * 根据状态获取项目列表
     */
    public List<ProjectEntity> getProjectsByStatus(String status) {
        logger.debug("按状态查询项目，状态: {}", status);
        
        LambdaQueryWrapper<ProjectEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectEntity::getStatus, status)
                   .orderByDesc(ProjectEntity::getCreatedAt);
        
        return projectRepository.selectList(queryWrapper);
    }

    /**
     * 删除项目（管理员权限）
     * 会级联删除相关的API实例和指标数据
     */
    public void deleteProject(String projectId) {
        logger.warn("删除项目，项目ID: {}", projectId);
        
        // 先验证项目是否存在
        validateProjectExists(projectId);
        
        // TODO: 在实际实现中，这里应该级联删除：
        // 1. 删除项目下的所有API实例
        // 2. 删除相关的指标数据
        // 3. 删除项目记录
        
        // 目前只删除项目记录
        int deleted = projectRepository.deleteById(projectId);
        if (deleted > 0) {
            logger.warn("项目删除成功，项目ID: {}", projectId);
        } else {
            throw new BusinessException("PROJECT_DELETE_FAILED", "项目删除失败，项目ID: " + projectId);
        }
    }

    /**
     * 获取项目统计信息
     */
    public Object getProjectStatistics() {
        logger.debug("获取项目统计信息");
        
        // TODO: 实现项目统计信息获取
        // 可以包括：总项目数、活跃项目数、各状态项目分布等
        
        return null;
    }

    /**
     * 根据API Key获取项目ID
     * 因为项目关联API Key，所以在项目表中查找
     */
    public String getProjectIdByApiKey(String apiKeyValue) {
        logger.debug("根据API Key查找项目ID: {}", apiKeyValue);
        
        LambdaQueryWrapper<ProjectEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ProjectEntity::getApiKey, apiKeyValue);
        
        ProjectEntity project = projectRepository.selectOne(queryWrapper);
        if (project == null) {
            logger.debug("未找到使用此API Key的项目: {}", apiKeyValue);
            return null;
        }
        
        logger.debug("找到项目: ID={}, Name={}", project.getId(), project.getName());
        return project.getId();
    }
} 