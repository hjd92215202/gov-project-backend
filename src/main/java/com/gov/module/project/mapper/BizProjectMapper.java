package com.gov.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.module.project.entity.BizProject;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工程项目 Mapper 接口
 */
@Mapper
public interface BizProjectMapper extends BaseMapper<BizProject> {
    // 所有的 CRUD 逻辑已经由 MyBatis-Plus 的 BaseMapper 提供
}