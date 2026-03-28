package com.gov.module.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.system.entity.SysUserRole;
import com.gov.module.system.mapper.SysUserRoleMapper;
import com.gov.module.system.service.SysUserRoleService;
import org.springframework.stereotype.Service;

/**
 * 用户角色关系服务实现。
 * 负责用户与角色关系的持久化操作。
 */
@Service
public class SysUserRoleServiceImpl extends ServiceImpl<SysUserRoleMapper, SysUserRole> implements SysUserRoleService {
}
