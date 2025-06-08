package org.xhy.gateway.application.assembler;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.xhy.gateway.application.dto.ProjectDTO;
import org.xhy.gateway.application.dto.ProjectSimpleDTO;
import org.xhy.gateway.domain.project.entity.ProjectEntity;
import org.xhy.gateway.interfaces.api.request.ProjectCreateRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 项目装配器
 * 负责 Entity 和 DTO 之间的转换
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class ProjectAssembler {

    /**
     * Request 转换为 Entity (用于创建)
     */
    public ProjectEntity toEntity(ProjectCreateRequest request) {
        if (request == null) {
            return null;
        }

        return new ProjectEntity(
                request.getName(),
                request.getDescription(),
                request.getApiKey()
        );
    }

    /**
     * Entity 转换为 DTO
     */
    public ProjectDTO toDTO(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }

        ProjectDTO dto = new ProjectDTO();
        BeanUtils.copyProperties(entity, dto);
        return dto;
    }

    /**
     * Entity 列表转换为 DTO 列表
     */
    public List<ProjectDTO> toDTOList(List<ProjectEntity> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Entity 转换为简化 DTO
     */
    public ProjectSimpleDTO toSimpleDTO(ProjectEntity entity) {
        if (entity == null) {
            return null;
        }

        return new ProjectSimpleDTO(entity.getId(), entity.getName());
    }

    /**
     * Entity 列表转换为简化 DTO 列表
     */
    public List<ProjectSimpleDTO> toSimpleDTOList(List<ProjectEntity> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toSimpleDTO)
                .collect(Collectors.toList());
    }
} 