package com.gov.module.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.SmUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gov.module.system.entity.SysRole;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.entity.SysUserRole;
import com.gov.module.system.mapper.SysRoleMapper;
import com.gov.module.system.mapper.SysUserMapper;
import com.gov.module.system.mapper.SysUserRoleMapper;
import com.gov.module.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Override
    public String login(String username, String password) {
        // 1. 根据用户名查询用户信息
        SysUser user = this.getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username.trim()));

        // 2. 校验用户是否存在及状态
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }
        if (user.getStatus() == 0) {
            throw new RuntimeException("账号已被停用");
        }

        // 3. 密码校验 (使用国密 SM3)
        // 注意：实际开发中通常使用 [密码+盐值] 的方式，这里我们先简单使用 [密码+用户名] 作为盐值
        String salt = user.getUsername().trim();
        String encryptPassword = SmUtil.sm3(password.trim() + salt);

        if (!encryptPassword.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 4. 登录验证：使用 Sa-Token
        StpUtil.login(user.getId());

        // 5. 返回 Token 给前端
        return StpUtil.getTokenValue();
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public List<String> getRoleCodes(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        List<Long> roleIds = getRoleIds(userId);
        if (roleIds.isEmpty()) {
            if (Long.valueOf(1L).equals(userId)) {
                return new ArrayList<>(Collections.singletonList("admin"));
            }
            return new ArrayList<>();
        }
        return sysRoleMapper.selectBatchIds(roleIds).stream()
                .map(SysRole::getRoleCode)
                .filter(Objects::nonNull)
                .map(this::normalizeRoleCode)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> getRoleIds(Long userId) {
        if (userId == null) {
            return new ArrayList<>();
        }
        return sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId))
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
        return getRoleCodes(userId).contains("dept_leader");
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
