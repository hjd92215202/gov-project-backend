package com.gov.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gov.module.system.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 职责：承载审计日志表 `sys_audit_log` 的基础数据库访问能力。
 * 为什么存在：让审计日志写入与分页查询复用 MyBatis-Plus 通用能力。
 * 关键输入输出：输入为审计实体，输出为持久化结果或分页记录。
 * 关联链路：AuditAccessFilter、SysAuditController。
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
