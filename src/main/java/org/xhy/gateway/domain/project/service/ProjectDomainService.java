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
} 