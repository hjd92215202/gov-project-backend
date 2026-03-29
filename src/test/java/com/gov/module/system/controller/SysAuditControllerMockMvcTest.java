package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.module.system.entity.SysAuditLog;
import com.gov.module.system.service.SysAuditLogService;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.UserAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 职责：验证审计日志控制器在 Web 层的权限与返回契约。
 * 为什么存在：该接口承担管理员审计能力，权限和分页结构需要被测试锁定。
 * 关键输入输出：输入为分页参数，输出为统一 `R` 包装的分页结果。
 * 关联链路：系统管理 -> 审计日志页面。
 */
class SysAuditControllerMockMvcTest {

    private final SysAuditLogService sysAuditLogService = mock(SysAuditLogService.class);
    private final SysUserService sysUserService = mock(SysUserService.class);
    private final SysDeptService sysDeptService = mock(SysDeptService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SysAuditController controller = new SysAuditController();
        ReflectionTestUtils.setField(controller, "sysAuditLogService", sysAuditLogService);
        ReflectionTestUtils.setField(controller, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(controller, "sysDeptService", sysDeptService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 作用：非管理员访问审计分页应返回 403 业务错误。
     */
    @Test
    void page_shouldRejectNonAdmin() throws Exception {
        UserAccessContext accessContext = new UserAccessContext();
        accessContext.setAdmin(false);
        when(sysUserService.getAccessContext(2L)).thenReturn(accessContext);

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(2L);

            mockMvc.perform(get("/system/audit/page?pageNum=1&pageSize=20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403));
        }
    }

    /**
     * 作用：管理员访问审计分页时，应返回用户补全后的列表结构。
     */
    @Test
    void page_shouldReturnAuditPageForAdmin() throws Exception {
        UserAccessContext accessContext = new UserAccessContext();
        accessContext.setAdmin(true);
        when(sysUserService.getAccessContext(1L)).thenReturn(accessContext);

        SysAuditLog log = new SysAuditLog();
        log.setId(9L);
        log.setUserId(null);
        log.setRequestMethod("POST");
        log.setRequestUri("/api/flow/approve");
        log.setClientIp("10.10.10.10");
        log.setHttpStatus(200);
        log.setDurationMs(120L);
        log.setTraceId("trace-demo");
        log.setUserAgent("JUnit-Agent");
        log.setRequestTime(new Date());

        IPage<SysAuditLog> page = new Page<>(1, 20, 1);
        page.setRecords(Collections.singletonList(log));
        when(sysAuditLogService.page(any(), any())).thenReturn(page);

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(get("/system/audit/page?pageNum=1&pageSize=20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].id").value(9))
                    .andExpect(jsonPath("$.data.records[0].userId").isEmpty())
                    .andExpect(jsonPath("$.data.records[0].username").isEmpty())
                    .andExpect(jsonPath("$.data.records[0].deptName").isEmpty())
                    .andExpect(jsonPath("$.data.records[0].requestMethod").value("POST"));
        }
    }

    /**
     * 作用：当审计表尚未初始化或查询异常时，接口应返回明确中文提示，避免前端出现“点击无反应”体感。
     */
    @Test
    void page_shouldReturnFriendlyMessageWhenQueryFailed() throws Exception {
        UserAccessContext accessContext = new UserAccessContext();
        accessContext.setAdmin(true);
        when(sysUserService.getAccessContext(1L)).thenReturn(accessContext);
        when(sysAuditLogService.page(any(), any())).thenThrow(new RuntimeException("table not found"));

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(get("/system/audit/page?pageNum=1&pageSize=20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(500))
                    .andExpect(jsonPath("$.msg").value("审计日志表尚未初始化，请重启后端服务后重试"));
        }
    }
}
