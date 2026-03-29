package com.gov.module.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.crypto.PasswordCrypto;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysRole;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.entity.SysUserRole;
import com.gov.module.system.mapper.SysRoleMapper;
import com.gov.module.system.mapper.SysUserMapper;
import com.gov.module.system.mapper.SysUserRoleMapper;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.UserAccessContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户服务实现。
 * 这一层除了基础用户读写，还额外承担登录校验、角色标准化、菜单聚合和访问上下文构建，
 * 是整个权限链路里最核心的汇总点之一。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    /** 普通用户默认菜单。 */
    private static final List<String> USER_DEFAULT_MENUS = Arrays.asList("project:manage");
    /** 部门负责人默认菜单。 */
    private static final List<String> DEPT_LEADER_DEFAULT_MENUS = Arrays.asList(
            "project:manage", "project:engineering", "system:user", "system:dept"
    );
    /** 管理员默认菜单。 */
    private static final List<String> ADMIN_DEFAULT_MENUS = Arrays.asList(
            "dashboard:view", "project:manage", "project:engineering", "system:user", "system:dept", "system:role", "system:audit"
    );

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysDeptService sysDeptService;

    /**
     * 执行登录校验。
     * 这里除了校验密码，还会检查启停状态，并在成功后交给 Sa-Token 建立登录态。
     */
    @Override
    public String login(String username, String password) {
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username.trim()));
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已停用");
        }

        if (!PasswordCrypto.matches(password, user.getUsername(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    /** 清理当前登录态。 */
    @Override
    public void logout() {
        StpUtil.logout();
    }

    /** 基于统一访问上下文获取角色编码，避免重复拼装。 */
    @Override
    public List<String> getRoleCodes(Long userId) {
        return getAccessContext(userId).getRoleCodes();
    }

    /** 基于统一访问上下文获取菜单键集合。 */
    @Override
    public List<String> getMenuKeys(Long userId) {
        return getAccessContext(userId).getMenuKeys();
    }

    /** 获取单个用户绑定的角色 ID。 */
    @Override
    public List<Long> getRoleIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(getRoleIdsMap(Collections.singletonList(userId))
                .getOrDefault(userId, Collections.emptyList()));
    }

    /**
     * 覆盖式分配角色。
     * 先清空旧关系，再按去重后的角色列表重新写入中间表。
     */
    @Override
    public void assignRoles(Long userId, List<Long> roleIds) {
        if (userId == null) {
            return;
        }
        sysUserRoleMapper.delete(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        Set<Long> uniqueRoleIds = roleIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        for (Long roleId : uniqueRoleIds) {
            SysUserRole userRole = new SysUserRole();
            userRole.setUserId(userId);
            userRole.setRoleId(roleId);
            sysUserRoleMapper.insert(userRole);
        }
    }

    /** 判断用户是否具备管理员语义。 */
    @Override
    public boolean isAdmin(Long userId) {
        return getAccessContext(userId).isAdmin();
    }

    /** 判断用户是否具备部门负责人语义。 */
    @Override
    public boolean isDeptLeader(Long userId) {
        return getAccessContext(userId).isDeptLeader();
    }

    /**
     * 构建统一访问上下文。
     * 这是性能优化后的关键入口，会一次性归集角色、菜单、部门和身份判断结果，
     * 让控制器和服务在同一请求里不再反复查角色、菜单和部门负责人关系。
     */
    @Override
    public UserAccessContext getAccessContext(Long userId) {
        UserAccessContext context = new UserAccessContext();
        context.setUserId(userId);
        if (userId == null) {
            context.setRoleCodes(new ArrayList<>(Collections.singletonList("user")));
            context.setMenuKeys(new ArrayList<>(USER_DEFAULT_MENUS));
            return context;
        }

        SysUser user = getById(userId);
        if (user != null) {
            context.setDeptId(user.getDeptId());
        }

        List<Long> roleIds = new ArrayList<>(getRoleIdsMap(Collections.singletonList(userId))
                .getOrDefault(userId, Collections.emptyList()));
        context.setRoleIds(roleIds);

        List<SysRole> roles = roleIds.isEmpty() ? new ArrayList<>() : sysRoleMapper.selectBatchIds(roleIds);
        LinkedHashSet<String> roleCodes = roles.stream()
                .map(SysRole::getRoleCode)
                .filter(Objects::nonNull)
                .map(this::normalizeRoleCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (roleCodes.isEmpty() && Long.valueOf(1L).equals(userId)) {
            roleCodes.add("admin");
        }
        roleCodes.add("user");

        context.setRoleCodes(new ArrayList<>(roleCodes));
        context.setAdmin(roleCodes.contains("admin"));

        boolean deptLeader = roleCodes.contains("dept_leader");
        if (!deptLeader) {
            deptLeader = sysDeptService.count(new LambdaQueryWrapper<SysDept>()
                    .eq(SysDept::getLeaderId, userId)) > 0;
        }
        context.setDeptLeader(deptLeader);

        LinkedHashSet<String> configuredMenus = new LinkedHashSet<>();
        for (SysRole role : roles) {
            if (role == null || StrUtil.isBlank(role.getMenuPerms())) {
                continue;
            }
            Arrays.stream(role.getMenuPerms().split(","))
                    .map(String::trim)
                    .filter(StrUtil::isNotBlank)
                    .forEach(configuredMenus::add);
        }

        if (!configuredMenus.isEmpty()) {
            context.setMenuKeys(new ArrayList<>(configuredMenus));
            return context;
        }

        LinkedHashSet<String> menuKeys = new LinkedHashSet<>();
        if (context.isAdmin()) {
            menuKeys.addAll(ADMIN_DEFAULT_MENUS);
        } else if (context.isDeptLeader()) {
            menuKeys.addAll(DEPT_LEADER_DEFAULT_MENUS);
        } else {
            menuKeys.addAll(USER_DEFAULT_MENUS);
        }
        context.setMenuKeys(new ArrayList<>(menuKeys));
        return context;
    }

    /**
     * 批量查询用户角色关系。
     * 主要给分页列表等批量场景使用，避免逐行查询角色造成 N+1。
     */
    @Override
    public Map<Long, List<Long>> getRoleIdsMap(Collection<Long> userIds) {
        Map<Long, List<Long>> roleIdsMap = new HashMap<>();
        if (userIds == null || userIds.isEmpty()) {
            return roleIdsMap;
        }

        List<Long> normalizedUserIds = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (normalizedUserIds.isEmpty()) {
            return roleIdsMap;
        }

        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getUserId, normalizedUserIds)
        );
        for (SysUserRole userRole : userRoles) {
            if (userRole.getUserId() == null || userRole.getRoleId() == null) {
                continue;
            }
            roleIdsMap.computeIfAbsent(userRole.getUserId(), key -> new ArrayList<>()).add(userRole.getRoleId());
        }
        for (Long id : normalizedUserIds) {
            roleIdsMap.computeIfAbsent(id, key -> new ArrayList<>());
        }
        return roleIdsMap;
    }

    /**
     * 标准化角色编码。
     * 这样可以兼容历史库里不同写法的角色编码，最终统一收敛到权限链路认识的标准值。
     */
    private String normalizeRoleCode(String roleCode) {
        if (roleCode == null) {
            return null;
        }
        String code = roleCode.trim().toLowerCase(Locale.ROOT);
        if (code.isEmpty()) {
            return null;
        }
        if ("admin".equals(code) || "administrator".equals(code) || "super_admin".equals(code)
                || "superadmin".equals(code) || "role_admin".equals(code)) {
            return "admin";
        }
        if ("dept_leader".equals(code) || "deptleader".equals(code) || "department_leader".equals(code)
                || "leader".equals(code) || "role_dept_leader".equals(code)) {
            return "dept_leader";
        }
        if ("user".equals(code) || "normal_user".equals(code) || "role_user".equals(code)) {
            return "user";
        }
        return code;
    }
}
