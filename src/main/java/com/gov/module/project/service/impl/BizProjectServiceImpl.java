package com.gov.module.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.project.entity.BizProject;
import com.gov.module.project.mapper.BizProjectMapper;
import com.gov.module.project.service.BizProjectService;
import org.springframework.stereotype.Service;

/**
 * 工程项目 Service 实现类
 */
@Service
public class BizProjectServiceImpl extends ServiceImpl<BizProjectMapper, BizProject> implements BizProjectService {
    // 基础的保存(save)、更新(updateById)、查询(getById) 已自动拥有
}