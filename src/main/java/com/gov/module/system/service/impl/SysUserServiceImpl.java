package com.gov.module.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.system.entity.SysRole;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.entity.SysUserRole;
import com.gov.module.system.mapper.SysRoleMapper;
import com.gov.module.system.mapper.SysUserMapper;
import com.gov.module.system.mapper.SysUserRoleMapper;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final List<String> USER_DEFAULT_MENUS = Arrays.asList("project:manage");
    private static final List<String> DEPT_LEADER_DEFAULT_MENUS = Arrays.asList(
            "project:manage", "project:engineering", "system:user", "system:dept"
    );
    private static final List<String> ADMIN_DEFAULT_MENUS = Arrays.asList(
            "dashboard:view", "project:manage", "project:engineering", "system:user", "system:dept", "system:role"
    );

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysDeptService sysDeptService;

    @Override
    public String login(String username, String password) {
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username.trim()));
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被禁用");
        }

        String salt = user.getUsername().trim();
        String encryptPassword = SmUtil.sm3(password.trim() + salt);
        if (!encryptPassword.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
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
        if (userId == null) {
            return new ArrayList<>(Collections.singletonList("user"));
        }

        List<Long> roleIds = getRoleIds(userId);
        if (roleIds.isEmpty()) {
            if (Long.valueOf(1L).equals(userId)) {
                return new ArrayList<>(Arrays.asList("admin", "user"));
            }
            return new ArrayList<>(Collections.singletonList("user"));
        }

        LinkedHashSet<String> roleCodes = sysRoleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode)
                .filter(Objects::nonNull)
                .map(this::normalizeRoleCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        roleCodes.add("user");
        return new ArrayList<>(roleCodes);
    }

    @Override
    public List<String> getMenuKeys(Long userId) {
        Set<String> roleCodes = new LinkedHashSet<>(getRoleCodes(userId));
        LinkedHashSet<String> configuredMenus = new LinkedHashSet<>();

        List<Long> roleIds = getRoleIds(userId);
        if (!roleIds.isEmpty()) {
            List<SysRole> roles = sysRoleMapper.selectBatchIds(roleIds);
            for (SysRole role : roles) {
                if (role == null || StrUtil.isBlank(role.getMenuPerms())) {
                    continue;
                }
                Arrays.stream(role.getMenuPerms().split(","))
                        .map(String::trim)
                        .filter(StrUtil::isNotBlank)
                        .forEach(configuredMenus::add);
            }
        }

        if (!configuredMenus.isEmpty()) {
            return new ArrayList<>(configuredMenus);
        }

        LinkedHashSet<String> menuKeys = new LinkedHashSet<>();
        if (roleCodes.contains("admin")) {
            menuKeys.addAll(ADMIN_DEFAULT_MENUS);
        } else if (roleCodes.contains("dept_leader")) {
            menuKeys.addAll(DEPT_LEADER_DEFAULT_MENUS);
        } else {
            menuKeys.addAll(USER_DEFAULT_MENUS);
        }
        return new ArrayList<>(menuKeys);
    }

    @Override
    public List<Long> getRoleIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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
        return getRoleCodes(userId).contains("admin");
    }

    @Override
    public boolean isDeptLeader(Long userId) {
        if (userId == null) {
            return false;
        }
        if (getRoleCodes(userId).contains("dept_leader")) {
            return true;
        }
        return sysDeptService.count(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getLeaderId, userId)) > 0;
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
}
