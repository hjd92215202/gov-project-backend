package com.gov.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.module.system.entity.SysAuditLog;

/**
 * 职责：定义审计日志读写服务契约。
 * 为什么存在：过滤器负责采集请求上下文，服务层负责同步或异步持久化，
 * 这样可以把“主链路响应”与“审计日志落库”解耦。
 */
public interface SysAuditLogService extends IService<SysAuditLog> {

    /**
     * 异步写入审计日志，避免主请求线程等待数据库落库完成。
     *
     * @param auditLog 审计日志实体
     */
    void saveAsync(SysAuditLog auditLog);
}
