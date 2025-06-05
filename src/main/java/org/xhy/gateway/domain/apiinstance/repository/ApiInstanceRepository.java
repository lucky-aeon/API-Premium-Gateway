package org.xhy.gateway.domain.apiinstance.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.gateway.domain.apiinstance.entity.ApiInstanceEntity;

/**
 * API实例仓储接口
 * 使用 MyBatis Plus BaseMapper，禁止手写 SQL
 * 
 * @author xhy
 * @since 1.0.0
 */
@Mapper
public interface ApiInstanceRepository extends BaseMapper<ApiInstanceEntity> {

} 