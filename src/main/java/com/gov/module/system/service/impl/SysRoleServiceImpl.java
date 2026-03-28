package com.gov.module.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.system.entity.SysRole;
import com.gov.module.system.mapper.SysRoleMapper;
import com.gov.module.system.service.SysRoleService;
import org.springframework.stereotype.Service;

/**
 * 角色服务实现。
 * 当前主要继承通用 CRUD 能力，后续如果角色逻辑进一步下沉，可继续扩展这里。
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
}
