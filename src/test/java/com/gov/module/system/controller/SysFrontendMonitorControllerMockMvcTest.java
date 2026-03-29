package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.module.system.entity.SysFrontendLog;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysFrontendLogService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.UserAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 职责：验证前端监控控制器在 Web 层的权限与上报契约。
 * 为什么存在：该接口承担浏览器运行监控的落库与管理员查询，权限和字段结构需要被测试锁定。
 * 关键输入输出：输入为上报 DTO 或分页参数，输出为统一 `R` 包装结果。
 * 关联链路：前端监控上报、系统设置 -> 前端监控页面。
 */
class SysFrontendMonitorControllerMockMvcTest {

    private final SysFrontendLogService sysFrontendLogService = mock(SysFrontendLogService.class);
    private final SysUserService sysUserService = mock(SysUserService.class);
    private final SysDeptService sysDeptService = mock(SysDeptService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        SysFrontendMonitorController controller = new SysFrontendMonitorController();
        ReflectionTestUtils.setField(controller, "sysFrontendLogService", sysFrontendLogService);
        ReflectionTestUtils.setField(controller, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(controller, "sysDeptService", sysDeptService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 作用：非管理员访问前端监控分页应返回 403 业务错误。
     */
    @Test
    void page_shouldRejectNonAdmin() throws Exception {
        UserAccessContext accessContext = new UserAccessContext();
        accessContext.setAdmin(false);
        when(sysUserService.getAccessContext(2L)).thenReturn(accessContext);

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(2L);

            mockMvc.perform(get("/system/frontend-monitor/page?pageNum=1&pageSize=20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403));
        }
    }

    /**
     * 作用：管理员访问前端监控分页时，应返回监控日志列表结构。
     */
    @Test
    void page_shouldReturnFrontendMonitorPageForAdmin() throws Exception {
        UserAccessContext accessContext = new UserAccessContext();
        accessContext.setAdmin(true);
        accessContext.setUserId(1L);
        when(sysUserService.getAccessContext(1L)).thenReturn(accessContext);

        SysFrontendLog log = new SysFrontendLog();
        log.setId(9L);
        log.setUserId(null);
        log.setLogLevel("warn");
        log.setLogType("request");
        log.setEventName("request");
        log.setMessage("检测到慢请求");
        log.setPagePath("/project/manage");
        log.setTraceId("trace-demo");
        log.setClientIp("10.10.10.10");
        log.setUserAgent("JUnit-Agent");
        log.setCreatedTime(new Date());

        IPage<SysFrontendLog> page = new Page<SysFrontendLog>(1, 20, 1);
        page.setRecords(Collections.singletonList(log));
        when(sysFrontendLogService.page(any(), any())).thenReturn(page);

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(get("/system/frontend-monitor/page?pageNum=1&pageSize=20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.records[0].id").value(9))
                    .andExpect(jsonPath("$.data.records[0].logLevel").value("warn"))
                    .andExpect(jsonPath("$.data.records[0].logType").value("request"))
                    .andExpect(jsonPath("$.data.records[0].message").value("检测到慢请求"));
        }
    }

    /**
     * 作用：前端上报监控日志时，应按批量日志保存到后端。
     */
    @Test
    void report_shouldPersistFrontendLogs() throws Exception {
        when(sysFrontendLogService.saveBatch(any())).thenReturn(true);
        String body = "{\"logs\":[{\"time\":\"2026-03-30T12:00:00Z\",\"level\":\"warn\",\"type\":\"request\",\"eventName\":\"request\",\"message\":\"检测到慢请求\",\"pagePath\":\"/project/manage\",\"traceId\":\"trace-demo\",\"detailJson\":\"{\\\"url\\\":\\\"/api/project/page\\\"}\"}]}";

        try (MockedStatic<StpUtil> stpUtil = mockStatic(StpUtil.class)) {
            stpUtil.when(StpUtil::getLoginIdDefaultNull).thenReturn(1L);

            mockMvc.perform(post("/system/frontend-monitor/report")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200));
        }

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SysFrontendLog>> captor = ArgumentCaptor.forClass((Class) List.class);
        verify(sysFrontendLogService).saveBatch(captor.capture());
        List<SysFrontendLog> savedLogs = captor.getValue();
        assertEquals(1, savedLogs.size());
        assertEquals("warn", savedLogs.get(0).getLogLevel());
        assertEquals("request", savedLogs.get(0).getLogType());
        assertEquals("/project/manage", savedLogs.get(0).getPagePath());
        assertNotNull(savedLogs.get(0).getCreatedTime());
    }
}
