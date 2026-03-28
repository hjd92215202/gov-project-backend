package com.gov.module.project.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.module.project.entity.BizProject;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目 Mapper。
 * 负责项目表与数据库之间的底层映射。
 */
@Mapper
public interface BizProjectMapper extends BaseMapper<BizProject> {
}
