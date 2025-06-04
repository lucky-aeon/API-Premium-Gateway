package org.xhy.gateway.domain.project.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.xhy.gateway.domain.project.entity.ProjectEntity;

/**
 * 项目仓储接口
 * 使用 MyBatis Plus BaseMapper，禁止手写 SQL
 * 
 * @author xhy
 * @since 1.0.0
 */
@Mapper
public interface ProjectRepository extends BaseMapper<ProjectEntity> {

} 