package org.xhy.gateway.domain.apikey.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.gateway.domain.apikey.entity.ApiKeyEntity;

/**
 * API Key 仓储接口
 * 使用 MyBatis Plus BaseMapper，禁止手写 SQL
 * 
 * @author xhy
 * @since 1.0.0
 */
@Mapper
public interface ApiKeyRepository extends BaseMapper<ApiKeyEntity> {

} 