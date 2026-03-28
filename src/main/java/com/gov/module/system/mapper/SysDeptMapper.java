package com.gov.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.module.system.entity.SysDept;
import org.apache.ibatis.annotations.Mapper;

/**
 * 部门 Mapper。
 * 负责部门表的底层数据库读写。
 */
@Mapper
public interface SysDeptMapper extends BaseMapper<SysDept> {
}
