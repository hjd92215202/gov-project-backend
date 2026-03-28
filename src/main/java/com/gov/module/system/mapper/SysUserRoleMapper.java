package com.gov.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.module.system.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户角色关系 Mapper。
 * 负责 `sys_user_role` 中间表的底层数据库读写。
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {
}
