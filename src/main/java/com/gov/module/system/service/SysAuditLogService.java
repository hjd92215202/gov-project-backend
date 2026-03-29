package com.gov.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.module.system.entity.SysAuditLog;

/**
 * 职责：定义审计日志读写服务契约。
 * 为什么存在：隔离控制器/过滤器对底层 Mapper 的直接依赖。
 * 关键输入输出：输入为审计实体或查询条件，输出为保存结果与分页数据。
 * 关联链路：AuditAccessFilter、SysAuditController。
 */
public interface SysAuditLogService extends IService<SysAuditLog> {
}
