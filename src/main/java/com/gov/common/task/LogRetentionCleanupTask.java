package com.gov.common.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.module.system.entity.SysAuditLog;
import com.gov.module.system.entity.SysFrontendLog;
import com.gov.module.system.service.SysAuditLogService;
import com.gov.module.system.service.SysFrontendLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/**
 * 职责：定期清理数据库中的审计日志和前端监控日志。
 * 为什么存在：文件日志有滚动策略，数据库日志也需要保留期治理，避免长期运行后表体积持续膨胀。
 * 关键输入输出：输入为保留天数和定时表达式配置，输出为过期日志删除动作与清理日志。
 * 关联链路：sys_audit_log、sys_frontend_log。
 */
@Component
public class LogRetentionCleanupTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogRetentionCleanupTask.class);

    @Autowired
    private SysAuditLogService sysAuditLogService;

    @Autowired
    private SysFrontendLogService sysFrontendLogService;

    @Value("${gov.logging.audit-retain-days:180}")
    private int auditRetainDays;

    @Value("${gov.logging.frontend-monitor-retain-days:30}")
    private int frontendMonitorRetainDays;

    /**
     * 职责：按配置定期清理过期数据库日志。
     * 为什么存在：避免日志表无限膨胀，保证检索效率和数据库存储可控。
     * 关键输入输出：输入为当前时间和保留天数，输出为删除结果日志。
     * 关联链路：数据库日志保留策略。
     */
    @Scheduled(cron = "${gov.logging.cleanup-cron:0 30 3 * * ?}")
    public void cleanupExpiredLogs() {
        cleanupAuditLogs();
        cleanupFrontendLogs();
    }

    private void cleanupAuditLogs() {
        if (auditRetainDays <= 0) {
            return;
        }
        Date deadline = resolveDeadline(auditRetainDays);
        long expiredCount = sysAuditLogService.count(new LambdaQueryWrapper<SysAuditLog>()
                .lt(SysAuditLog::getRequestTime, deadline));
        if (expiredCount <= 0) {
            return;
        }
        sysAuditLogService.remove(new LambdaQueryWrapper<SysAuditLog>()
                .lt(SysAuditLog::getRequestTime, deadline));
        LOGGER.info("审计日志保留期清理完成 retainDays={}, removed={}", auditRetainDays, expiredCount);
    }

    private void cleanupFrontendLogs() {
        if (frontendMonitorRetainDays <= 0) {
            return;
        }
        Date deadline = resolveDeadline(frontendMonitorRetainDays);
        long expiredCount = sysFrontendLogService.count(new LambdaQueryWrapper<SysFrontendLog>()
                .lt(SysFrontendLog::getCreatedTime, deadline));
        if (expiredCount <= 0) {
            return;
        }
        sysFrontendLogService.remove(new LambdaQueryWrapper<SysFrontendLog>()
                .lt(SysFrontendLog::getCreatedTime, deadline));
        LOGGER.info("前端监控日志保留期清理完成 retainDays={}, removed={}", frontendMonitorRetainDays, expiredCount);
    }

    private Date resolveDeadline(int retainDays) {
        return Date.from(Instant.now().minus(retainDays, ChronoUnit.DAYS));
    }
}
