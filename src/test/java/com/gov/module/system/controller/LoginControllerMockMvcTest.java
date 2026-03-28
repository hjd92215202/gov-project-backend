package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.UserAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 职责：验证登录控制器在 Web 层的响应结构。
 * 为什么存在：登录和 `/me` 是整个前端会话的起点，返回字段一旦变化会影响路由和权限恢复。
 * 关键输入输出：输入为登录 JSON 或 `/me` 请求，输出为包含角色与菜单的中文响应。
 * 关联链路：登录页、会话恢复、菜单权限初始化。
 */
@ExtendWith(MockitoExtension.class)
class LoginControllerMockMvcTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockMvc mockMvc;
    private LoginController controller;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysDeptService sysDeptService;

    @BeforeEach
    void setUp() {
        controller = new LoginController();
        ReflectionTestUtils.setField(controller, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(controller, "sysDeptService", sysDeptService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 作用：验证登录接口会返回 token、角色码和菜单权限等前端关键字段。
     */
    @Test
    void login_shouldReturnTokenAndCurrentUserPayload() throws Exception {
        Map<String, String> payload = new HashMap<String, String>();
        payload.put("username", "admin");
        payload.put("password", "secret");

        UserAccessContext context = new UserAccessContext();
        context.setDeptId(10L);
        context.setRoleCodes(Arrays.asList("admin", "user"));
        context.setMenuKeys(Arrays.asList("dashboard:view", "system:user"));

        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername("admin");
        user.setRealName("系统管理员");

        SysDept dept = new SysDept();
        dept.setId(10L);
        dept.setDeptName("综合管理部");

        when(sysUserService.login("admin", "secret")).thenReturn("token-1");
        when(sysUserService.getAccessContext(1L)).thenReturn(context);
        when(sysUserService.getById(1L)).thenReturn(user);
        when(sysDeptService.getById(10L)).thenReturn(dept);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            mockMvc.perform(post("/system/login")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.tokenValue").value("token-1"))
                    .andExpect(jsonPath("$.data.tokenName").value("Authorization"))
                    .andExpect(jsonPath("$.data.roleCodes", hasSize(2)))
                    .andExpect(jsonPath("$.data.menuKeys[0]").value("dashboard:view"));
        }
    }

    /**
     * 作用：验证 `/me` 接口会返回当前用户信息，但不会重复暴露 token 字段。
     */
    @Test
    void me_shouldReturnCurrentUserWithoutTokenFields() throws Exception {
        UserAccessContext context = new UserAccessContext();
        context.setDeptId(20L);
        context.setRoleCodes(Arrays.asList("dept_leader", "user"));
        context.setMenuKeys(Arrays.asList("project:engineering"));

        SysUser user = new SysUser();
        user.setId(8L);
        user.setUsername("leader");
        user.setRealName("部门负责人");

        when(sysUserService.getAccessContext(8L)).thenReturn(context);
        when(sysUserService.getById(8L)).thenReturn(user);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(8L);

            mockMvc.perform(get("/system/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.username").value("leader"))
                    .andExpect(jsonPath("$.data.tokenValue").doesNotExist());
        }
    }
}
