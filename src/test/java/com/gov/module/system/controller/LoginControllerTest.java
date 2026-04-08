package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.gov.common.result.R;
import com.gov.module.system.dto.LoginDTO;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginControllerTest {

    @InjectMocks
    private LoginController controller;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysDeptService sysDeptService;

    @Test
    void login_shouldReturnCurrentUserPayload() {
        LoginDTO payload = new LoginDTO();
        payload.setUsername("admin");
        payload.setPassword("secret");

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
            assertFalse(result.getData().containsKey("tokenValue"));
            assertEquals("综合管理部", result.getData().get("deptName"));
            assertEquals(Arrays.asList("admin", "user"), result.getData().get("roleCodes"));
        }
    }

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
