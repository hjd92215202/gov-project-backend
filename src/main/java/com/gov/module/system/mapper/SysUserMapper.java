package com.gov.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    // 基础的增删改查 MyBatis-Plus 已经帮我们写好了
}