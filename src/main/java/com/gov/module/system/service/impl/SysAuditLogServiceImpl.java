package com.gov.module.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.system.entity.SysAuditLog;
import com.gov.module.system.mapper.SysAuditLogMapper;
import com.gov.module.system.service.SysAuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 职责：提供审计日志读写与异步落库实现。
 * 为什么存在：审计日志必须可靠保留，但又不能让每一次接口点击都额外等待数据库写入完成。
 */
@Service
public class SysAuditLogServiceImpl extends ServiceImpl<SysAuditLogMapper, SysAuditLog> implements SysAuditLogService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SysAuditLogServiceImpl.class);

    @Value("${gov.logging.audit-persist-slow-ms:200}")
    private long auditPersistSlowThresholdMs;

    /**
     * 异步写入审计日志。
     * 这里仍然保留慢写入告警，便于观察数据库是否成为新的瓶颈。
     *
     * @param auditLog 审计日志实体
     */
    @Async("auditLogExecutor")
    @Override
    public void saveAsync(SysAuditLog auditLog) {
        if (auditLog == null) {
            return;
        }
        long startAt = System.currentTimeMillis();
        try {
            save(auditLog);
            long durationMs = System.currentTimeMillis() - startAt;
            if (durationMs >= auditPersistSlowThresholdMs) {
                LOGGER.warn("审计日志异步落库耗时偏高 uri={} traceId={} durationMs={} thresholdMs={}",
                        auditLog.getRequestUri(), auditLog.getTraceId(), durationMs, auditPersistSlowThresholdMs);
            }
        } catch (Exception exception) {
            LOGGER.warn("审计日志异步落库失败 uri={} traceId={} message={}",
                    auditLog.getRequestUri(), auditLog.getTraceId(), exception.getMessage());
        }
    }
}
