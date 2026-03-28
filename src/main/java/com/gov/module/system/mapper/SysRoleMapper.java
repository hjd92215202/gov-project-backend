package com.gov.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.module.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色 Mapper。
 * 负责角色表的底层数据库读写。
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
}
