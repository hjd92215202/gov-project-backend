package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.gov.common.result.R;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.UserAccessContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * 职责：验证登录控制器的纯业务组装逻辑。
 * 为什么存在：这层测试不依赖 MockMvc，更适合快速保护 token、部门名和菜单等组装结果。
 * 关键输入输出：输入为登录参数或当前登录态，输出为 `R<Map<String, Object>>`。
 * 关联链路：登录页成功跳转、会话刷新、首页路由解析。
 */
@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @InjectMocks
    private LoginController controller;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysDeptService sysDeptService;

    /**
     * 作用：验证登录返回会携带前端初始化所需的完整字段。
     */
    @Test
    void login_shouldReturnTokenAndCurrentUserPayload() {
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

            R<Map<String, Object>> result = controller.login(payload);

            assertEquals(Integer.valueOf(200), result.getCode());
            assertEquals("token-1", result.getData().get("tokenValue"));
            assertEquals("Authorization", result.getData().get("tokenName"));
            assertEquals("综合管理部", result.getData().get("deptName"));
            assertEquals(Arrays.asList("admin", "user"), result.getData().get("roleCodes"));
        }
    }

    /**
     * 作用：验证 `/me` 返回不会包含 token 字段，但会保留角色和菜单信息。
     */
    @Test
    void me_shouldReturnPayloadWithoutTokenFields() {
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

            R<Map<String, Object>> result = controller.me();

            assertEquals(Integer.valueOf(200), result.getCode());
            assertFalse(result.getData().containsKey("tokenValue"));
            assertEquals("leader", result.getData().get("username"));
            assertTrue(((java.util.List<?>) result.getData().get("menuKeys")).contains("project:engineering"));
        }
    }
}
