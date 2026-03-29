package com.gov.module.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.SmUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.module.system.entity.SysRole;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.entity.SysUserRole;
import com.gov.module.system.mapper.SysRoleMapper;
import com.gov.module.system.mapper.SysUserRoleMapper;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.vo.UserAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 职责：验证用户服务的高风险权限与登录逻辑。
 * 为什么存在：用户上下文聚合会影响菜单、数据权限和首页跳转，是系统权限链路的根部。
 * 关键输入输出：输入为用户、角色、角色关系和密码，输出为访问上下文或登录 token。
 * 关联链路：登录、菜单权限初始化、角色分配。
 */
@ExtendWith(MockitoExtension.class)
class SysUserServiceImplTest {

    private final TestableSysUserServiceImpl service = new TestableSysUserServiceImpl();

    @Mock
    private SysUserRoleMapper sysUserRoleMapper;

    @Mock
    private SysRoleMapper sysRoleMapper;

    @Mock
    private SysDeptService sysDeptService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "sysUserRoleMapper", sysUserRoleMapper);
        ReflectionTestUtils.setField(service, "sysRoleMapper", sysRoleMapper);
        ReflectionTestUtils.setField(service, "sysDeptService", sysDeptService);
        RequestContextHolder.resetRequestAttributes();
        service.userById = null;
        service.singleUser = null;
        service.roleIdsMap = Collections.emptyMap();
    }

    /**
     * 作用：验证超级管理员兜底逻辑仍然生效，避免管理员丢菜单。
     */
    @Test
    void getAccessContext_shouldFallbackToAdminForUserOne() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setDeptId(10L);

        service.userById = user;
        service.roleIdsMap = Collections.emptyMap();
        when(sysDeptService.count(any(LambdaQueryWrapper.class))).thenReturn(0L);

        UserAccessContext context = service.getAccessContext(1L);

        assertTrue(context.isAdmin());
        assertEquals(Long.valueOf(10L), context.getDeptId());
        assertTrue(context.getRoleCodes().contains("admin"));
        assertTrue(context.getRoleCodes().contains("user"));
        assertTrue(context.getMenuKeys().contains("dashboard:view"));
    }

    /**
     * 作用：验证当角色显式配置菜单时，会优先使用角色菜单而不是默认菜单集。
     */
    @Test
    void getAccessContext_shouldPreferConfiguredMenus() {
        SysUser user = new SysUser();
        user.setId(2L);
        user.setDeptId(20L);

        SysRole role = new SysRole();
        role.setId(9L);
        role.setRoleCode("dept_leader");
        role.setMenuPerms("project:engineering,system:user");

        service.userById = user;
        service.roleIdsMap = Collections.singletonMap(2L, Collections.singletonList(9L));
        when(sysRoleMapper.selectBatchIds(Collections.singletonList(9L))).thenReturn(Collections.singletonList(role));

        UserAccessContext context = service.getAccessContext(2L);

        assertTrue(context.isDeptLeader());
        assertIterableEquals(Arrays.asList("project:engineering", "system:user"), context.getMenuKeys());
    }

    /**
     * 作用：验证同一请求内重复读取访问上下文时，会直接复用缓存，避免重复查角色和部门负责人关系。
     */
    @Test
    void getAccessContext_shouldReuseRequestScopedCache() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        SysUser user = new SysUser();
        user.setId(6L);
        user.setDeptId(60L);

        SysRole role = new SysRole();
        role.setId(12L);
        role.setRoleCode("dept_leader");
        role.setMenuPerms("project:engineering,system:user");

        service.userById = user;
        service.roleIdsMap = Collections.singletonMap(6L, Collections.singletonList(12L));
        when(sysRoleMapper.selectBatchIds(Collections.singletonList(12L))).thenReturn(Collections.singletonList(role));

        UserAccessContext first = service.getAccessContext(6L);
        UserAccessContext second = service.getAccessContext(6L);

        assertEquals(first, second);
        verify(sysRoleMapper, times(1)).selectBatchIds(Collections.singletonList(12L));
    }

    /**
     * 作用：验证角色分配会先清旧关系，再去重插入新关系。
     */
    @Test
    void assignRoles_shouldReplaceOldRelationsAndDeduplicate() {
        when(sysUserRoleMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);

        service.assignRoles(100L, Arrays.asList(7L, 7L, 8L));

        ArgumentCaptor<SysUserRole> captor = ArgumentCaptor.forClass(SysUserRole.class);
        verify(sysUserRoleMapper, times(2)).insert(captor.capture());
        List<SysUserRole> inserted = captor.getAllValues();
        assertEquals(2, inserted.size());
        assertTrue(inserted.stream().anyMatch(item -> item.getUserId().equals(100L) && item.getRoleId().equals(7L)));
        assertTrue(inserted.stream().anyMatch(item -> item.getUserId().equals(100L) && item.getRoleId().equals(8L)));
    }

    /**
     * 作用：验证密码校验通过时，用户服务会完成登录并返回 token。
     */
    @Test
    void login_shouldCreateTokenWhenPasswordMatches() {
        SysUser user = new SysUser();
        user.setId(300L);
        user.setUsername("tester");
        user.setStatus(1);
        user.setPassword(SmUtil.sm3("secret" + "tester"));

        service.singleUser = user;

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getTokenValue).thenReturn("token-123");

            String token = service.login("tester", "secret");

            stp.verify(() -> StpUtil.login(300L));
            assertEquals("token-123", token);
        }
    }

    /**
     * 职责：提供一个可控的用户服务子类，用来隔离数据库和 MyBatis 行为。
     * 为什么存在：这能让测试只聚焦业务语义，不受具体 ORM 或字节码增强影响。
     */
    static class TestableSysUserServiceImpl extends SysUserServiceImpl {
        private SysUser userById;
        private SysUser singleUser;
        private Map<Long, List<Long>> roleIdsMap = Collections.emptyMap();

        @Override
        public SysUser getById(java.io.Serializable id) {
            return userById;
        }

        @Override
        public SysUser getOne(com.baomidou.mybatisplus.core.conditions.Wrapper<SysUser> queryWrapper) {
            return singleUser;
        }

        @Override
        public Map<Long, List<Long>> getRoleIdsMap(Collection<Long> userIds) {
            return roleIdsMap;
        }
    }
}
