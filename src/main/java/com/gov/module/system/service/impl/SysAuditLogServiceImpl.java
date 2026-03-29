package com.gov.module.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.system.entity.SysAuditLog;
import com.gov.module.system.mapper.SysAuditLogMapper;
import com.gov.module.system.service.SysAuditLogService;
import org.springframework.stereotype.Service;

/**
 * 职责：审计日志服务默认实现。
 * 为什么存在：复用 MyBatis-Plus 的通用 Service 能力，减少样板代码。
 * 关键输入输出：输入为审计日志实体与查询条件，输出为数据库读写结果。
 * 关联链路：AuditAccessFilter 写入、SysAuditController 查询。
 */
@Service
public class SysAuditLogServiceImpl extends ServiceImpl<SysAuditLogMapper, SysAuditLog> implements SysAuditLogService {
}
