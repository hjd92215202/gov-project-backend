package com.gov.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.module.system.entity.SysDept;

/**
 * 部门服务接口。
 * 组织树查询、负责人归属判断和审批逐级上报都依赖这一层的数据访问能力。
 */
public interface SysDeptService extends IService<SysDept> {
}
