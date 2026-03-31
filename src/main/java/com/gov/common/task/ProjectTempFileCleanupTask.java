package com.gov.common.task;

import com.gov.module.file.service.SysFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

/**
 * 项目临时附件清理任务。
 * 用来兜底处理用户中途关闭页面、浏览器崩溃等场景遗留的未绑定附件。
 */
@Component
public class ProjectTempFileCleanupTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectTempFileCleanupTask.class);

    @Autowired
    private SysFileService sysFileService;

    @Value("${gov.file.temp-retain-hours:24}")
    private int tempRetainHours;

    /**
     * 定时清理超出保留时长的临时附件。
     */
    @Scheduled(cron = "${gov.file.temp-cleanup-cron:0 0 4 * * ?}")
    public void cleanupExpiredTempFiles() {
        if (tempRetainHours <= 0) {
            return;
        }
        int removedCount = sysFileService.cleanupExpiredTemporaryFiles(resolveDeadline(tempRetainHours));
        if (removedCount <= 0) {
            return;
        }
        LOGGER.info("项目临时附件清理完成 retainHours={}, removed={}", tempRetainHours, removedCount);
    }

    private Date resolveDeadline(int retainHours) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.HOUR_OF_DAY, -retainHours);
        return calendar.getTime();
    }
}
