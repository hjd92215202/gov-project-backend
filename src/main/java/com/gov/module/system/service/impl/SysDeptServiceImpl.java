package com.gov.module.system.service.impl;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.mapper.SysDeptMapper;
import com.gov.module.system.service.SysDeptService;
import org.springframework.stereotype.Service;
@Service
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {}