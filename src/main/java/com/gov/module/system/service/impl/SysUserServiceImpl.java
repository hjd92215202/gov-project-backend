package com.gov.module.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.common.exception.BizException;
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
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

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
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final String ACCESS_CONTEXT_REQUEST_ATTR = SysUserServiceImpl.class.getName() + ".ACCESS_CONTEXT";
    private static final String INVALID_LOGIN_MESSAGE = "用户名或密码错误";

    private static final List<String> USER_DEFAULT_MENUS = Arrays.asList("project:manage");
    private static final List<String> DEPT_LEADER_DEFAULT_MENUS = Arrays.asList(
            "project:manage", "project:engineering", "system:user", "system:dept"
    );
    private static final List<String> ADMIN_DEFAULT_MENUS = Arrays.asList(
            "dashboard:view", "project:manage", "project:engineering", "system:user",
            "system:dept", "system:role", "system:audit", "system:frontend-monitor"
    );

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysDeptService sysDeptService;

    @Override
    public String login(String username, String password) {
        if (StrUtil.isBlank(username) || StrUtil.isBlank(password)) {
            throw new BizException(400, "用户名和密码不能为空");
        }

        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username.trim()));
        if (user == null || user.getStatus() == null || user.getStatus() == 0) {
            throw new BizException(401, INVALID_LOGIN_MESSAGE);
        }

        if (!PasswordCrypto.matches(password, user.getUsername(), user.getPassword())) {
            throw new BizException(401, INVALID_LOGIN_MESSAGE);
        }

        if (PasswordCrypto.needsUpgrade(user.getPassword())) {
            SysUser passwordUpdate = new SysUser();
            passwordUpdate.setId(user.getId());
            passwordUpdate.setPassword(PasswordCrypto.encode(password, user.getUsername()));
            if (this.baseMapper != null) {
                updateById(passwordUpdate);
            } else {
                user.setPassword(passwordUpdate.getPassword());
            }
        }

        StpUtil.login(user.getId());
        return StpUtil.getTokenValue();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        return getAccessContext(userId).getRoleCodes();
    }

    @Override
    public List<String> getMenuKeys(Long userId) {
        return getAccessContext(userId).getMenuKeys();
    }

    @Override
    public List<Long> getRoleIds(Long userId) {
        if (userId == null) {
            return new ArrayList<Long>();
        }
        return new ArrayList<Long>(getRoleIdsMap(Collections.singletonList(userId))
                .getOrDefault(userId, Collections.<Long>emptyList()));
    }

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

    @Override
    public boolean isAdmin(Long userId) {
        return getAccessContext(userId).isAdmin();
    }

    @Override
    public boolean isDeptLeader(Long userId) {
        return getAccessContext(userId).isDeptLeader();
    }

    @Override
    public UserAccessContext getAccessContext(Long userId) {
        UserAccessContext cachedContext = getCachedAccessContext(userId);
        if (cachedContext != null) {
            return cachedContext;
        }

        UserAccessContext context = new UserAccessContext();
        context.setUserId(userId);
        if (userId == null) {
            context.setRoleCodes(new ArrayList<String>(Collections.singletonList("user")));
            context.setMenuKeys(new ArrayList<String>(USER_DEFAULT_MENUS));
            cacheAccessContext(context);
            return context;
        }

        SysUser user = getById(userId);
        if (user != null) {
            context.setUsername(user.getUsername());
            context.setRealName(user.getRealName());
            context.setDeptId(user.getDeptId());
            if (user.getDeptId() != null) {
                SysDept dept = sysDeptService.getById(user.getDeptId());
                if (dept != null) {
                    context.setDeptName(dept.getDeptName());
                }
            }
        }

        List<Long> roleIds = new ArrayList<Long>(getRoleIdsMap(Collections.singletonList(userId))
                .getOrDefault(userId, Collections.<Long>emptyList()));
        context.setRoleIds(roleIds);

        List<SysRole> roles = roleIds.isEmpty() ? new ArrayList<SysRole>() : sysRoleMapper.selectBatchIds(roleIds);
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

        context.setRoleCodes(new ArrayList<String>(roleCodes));
        context.setAdmin(roleCodes.contains("admin"));

        boolean deptLeader = roleCodes.contains("dept_leader");
        if (!deptLeader) {
            deptLeader = sysDeptService.count(new LambdaQueryWrapper<SysDept>()
                    .eq(SysDept::getLeaderId, userId)) > 0;
        }
        context.setDeptLeader(deptLeader);

        LinkedHashSet<String> configuredMenus = new LinkedHashSet<String>();
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
            context.setMenuKeys(new ArrayList<String>(configuredMenus));
            cacheAccessContext(context);
            return context;
        }

        LinkedHashSet<String> menuKeys = new LinkedHashSet<String>();
        if (context.isAdmin()) {
            menuKeys.addAll(ADMIN_DEFAULT_MENUS);
        } else if (context.isDeptLeader()) {
            menuKeys.addAll(DEPT_LEADER_DEFAULT_MENUS);
        } else {
            menuKeys.addAll(USER_DEFAULT_MENUS);
        }
        context.setMenuKeys(new ArrayList<String>(menuKeys));
        cacheAccessContext(context);
        return context;
    }

    @Override
    public Map<Long, List<Long>> getRoleIdsMap(Collection<Long> userIds) {
        Map<Long, List<Long>> roleIdsMap = new HashMap<Long, List<Long>>();
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
            roleIdsMap.computeIfAbsent(userRole.getUserId(), key -> new ArrayList<Long>()).add(userRole.getRoleId());
        }
        for (Long id : normalizedUserIds) {
            roleIdsMap.computeIfAbsent(id, key -> new ArrayList<Long>());
        }
        return roleIdsMap;
    }

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

    private UserAccessContext getCachedAccessContext(Long userId) {
        ServletRequestAttributes attributes = currentRequestAttributes();
        if (attributes == null) {
            return null;
        }
        Object cached = attributes.getRequest().getAttribute(ACCESS_CONTEXT_REQUEST_ATTR);
        if (!(cached instanceof UserAccessContext)) {
            return null;
        }
        UserAccessContext context = (UserAccessContext) cached;
        if (Objects.equals(context.getUserId(), userId)) {
            return context;
        }
        return null;
    }

    private void cacheAccessContext(UserAccessContext context) {
        ServletRequestAttributes attributes = currentRequestAttributes();
        if (attributes == null || context == null) {
            return;
        }
        attributes.getRequest().setAttribute(ACCESS_CONTEXT_REQUEST_ATTR, context);
    }

    private ServletRequestAttributes currentRequestAttributes() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return (ServletRequestAttributes) attributes;
        }
        return null;
    }
}
