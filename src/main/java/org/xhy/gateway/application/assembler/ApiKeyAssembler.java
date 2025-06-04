package org.xhy.gateway.application.assembler;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.xhy.gateway.application.dto.ApiKeyDTO;
import org.xhy.gateway.domain.apikey.entity.ApiKeyEntity;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API Key 装配器
 * 负责 Entity 和 DTO 之间的转换
 */
@Component
public class ApiKeyAssembler {

    /**
     * Entity 转换为 DTO
     */
    public ApiKeyDTO toDTO(ApiKeyEntity entity) {
        if (entity == null) {
            return null;
        }

        ApiKeyDTO dto = new ApiKeyDTO();
        BeanUtils.copyProperties(entity, dto);
        // 枚举转字符串
        dto.setStatus(entity.getStatus().name());
        return dto;
    }

    /**
     * Entity 列表转换为 DTO 列表
     */
    public List<ApiKeyDTO> toDTOList(List<ApiKeyEntity> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }
} 