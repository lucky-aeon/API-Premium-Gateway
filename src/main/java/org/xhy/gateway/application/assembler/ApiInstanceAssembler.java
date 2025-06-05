package org.xhy.gateway.application.assembler;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.xhy.gateway.application.dto.ApiInstanceDTO;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;
import org.xhy.gateway.interfaces.api.request.api_instance.ApiInstanceCreateRequest;
import org.xhy.gateway.interfaces.api.request.api_instance.ApiInstanceUpdateRequest;

import java.util.List;
import java.util.stream.Collectors;

/**
 * API实例装配器
 * 负责Entity与DTO之间的转换
 * 
 * @author xhy
 * @since 1.0.0
 */
@Component
public class ApiInstanceAssembler {

    /**
     * 将实体转换为DTO
     */
    public static ApiInstanceDTO toDTO(ApiInstanceEntity entity) {

        ApiInstanceDTO dto = new ApiInstanceDTO();
        BeanUtils.copyProperties(entity, dto);

        return dto;
    }

    /**
     * 将实体列表转换为DTO列表
     */
    public static List<ApiInstanceDTO> toDTOList(List<ApiInstanceEntity> entities) {
        if (entities == null) {
            return null;
        }

        return entities.stream()
                .map(ApiInstanceAssembler::toDTO)
                .collect(Collectors.toList());
    }


    public static ApiInstanceEntity toEntity(ApiInstanceCreateRequest request) {
        ApiInstanceEntity entity = new ApiInstanceEntity();
        BeanUtils.copyProperties(request, entity);

        return entity;
    }

    public static ApiInstanceEntity toEntity(ApiInstanceUpdateRequest request) {
        ApiInstanceEntity entity = new ApiInstanceEntity();
        BeanUtils.copyProperties(request, entity);

        return entity;
    }


}