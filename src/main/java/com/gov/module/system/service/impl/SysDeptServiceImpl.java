package com.gov.module.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.mapper.SysDeptMapper;
import com.gov.module.system.service.SysDeptService;
import org.springframework.stereotype.Service;

/**
 * 部门服务实现。
 * 目前提供基础 CRUD，作为组织树和部门规则扩展的稳定落点。
 */
@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {
}
