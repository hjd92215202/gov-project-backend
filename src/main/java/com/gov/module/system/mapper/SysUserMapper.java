package com.gov.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 * 负责用户表的底层数据库读写。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
