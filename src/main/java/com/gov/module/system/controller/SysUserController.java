package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.common.result.R;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysRole;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysRoleService;
import com.gov.module.system.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Api(tags = "用户管理")
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

    @Autowired
    private SysRoleService sysRoleService;

    @ApiOperation("用户分页查询")
    @GetMapping("/page")
    public R<IPage<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId
    ) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = sysUserService.isAdmin(currentUserId);
        boolean isDeptLeader = sysUserService.isDeptLeader(currentUserId);
        if (!isAdmin && !isDeptLeader) {
            return R.fail(403, "无权限操作");
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getId, SysUser::getDeptId, SysUser::getUsername,
                SysUser::getRealName, SysUser::getPhone, SysUser::getStatus, SysUser::getCreateTime);
        wrapper.like(StrUtil.isNotBlank(username), SysUser::getUsername, username);
        wrapper.like(StrUtil.isNotBlank(realName), SysUser::getRealName, realName);
        wrapper.eq(status != null, SysUser::getStatus, status);

        if (isAdmin) {
            wrapper.eq(deptId != null, SysUser::getDeptId, deptId);
        } else {
            Set<Long> scopedDeptIds = getScopedDeptIds(currentUserId);
            if (scopedDeptIds.isEmpty()) {
                return R.fail(403, "当前用户未绑定部门");
            }
            wrapper.in(SysUser::getDeptId, scopedDeptIds);
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> page = sysUserService.page(new Page<>(pageNum, pageSize), wrapper);
        fillDeptName(page.getRecords());
        fillRoleNames(page.getRecords());
        return R.ok(page);
    }

    @ApiOperation("用户简表列表")
    @GetMapping("/simple")
    public R<List<SysUser>> simple() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = sysUserService.isAdmin(currentUserId);
        boolean isDeptLeader = sysUserService.isDeptLeader(currentUserId);

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getId, SysUser::getDeptId, SysUser::getUsername,
                SysUser::getRealName, SysUser::getPhone, SysUser::getStatus);
        wrapper.eq(SysUser::getStatus, 1);

        if (!isAdmin && isDeptLeader) {
            Set<Long> scopedDeptIds = getScopedDeptIds(currentUserId);
            if (scopedDeptIds.isEmpty()) {
                return R.fail(403, "当前用户未绑定部门");
            }
            wrapper.in(SysUser::getDeptId, scopedDeptIds);
        } else if (!isAdmin) {
            wrapper.eq(SysUser::getId, currentUserId);
        }

        wrapper.orderByAsc(SysUser::getId);
        return R.ok(sysUserService.list(wrapper));
    }

    @ApiOperation("新增用户")
    @PostMapping("/add")
    public R<Map<String, Object>> add(@RequestBody SysUser user) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = sysUserService.isAdmin(currentUserId);
        boolean isDeptLeader = sysUserService.isDeptLeader(currentUserId);
        if (!isAdmin && !isDeptLeader) {
            return R.fail(403, "无权限操作");
        }
        if (StrUtil.isBlank(user.getUsername())) {
            return R.fail("用户名不能为空");
        }

        String username = user.getUsername().trim();
        boolean exists = sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0;
        if (exists) {
            return R.fail("用户名已存在");
        }

        if (isDeptLeader && !isAdmin) {
            Set<Long> scopedDeptIds = getScopedDeptIds(currentUserId);
            if (user.getDeptId() == null) {
                SysUser currentUser = sysUserService.getById(currentUserId);
                user.setDeptId(currentUser == null ? null : currentUser.getDeptId());
            }
            if (user.getDeptId() == null || !scopedDeptIds.contains(user.getDeptId())) {
                return R.fail(403, "仅允许在权限范围内的部门操作");
            }
        }

        String rawPassword = StrUtil.isBlank(user.getPassword()) ? "123456" : user.getPassword().trim();
        user.setUsername(username);
        user.setPassword(SmUtil.sm3(rawPassword + username));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }

        sysUserService.save(user);
        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        return R.ok(data, "用户创建成功");
    }

    @ApiOperation("更新用户")
    @PutMapping("/update")
    public R<String> update(@RequestBody SysUser user) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = sysUserService.isAdmin(currentUserId);
        boolean isDeptLeader = sysUserService.isDeptLeader(currentUserId);
        if (!isAdmin && !isDeptLeader) {
            return R.fail(403, "无权限操作");
        }
        if (user.getId() == null) {
            return R.fail("用户ID不能为空");
        }

        SysUser dbUser = sysUserService.getById(user.getId());
        if (dbUser == null) {
            return R.fail("用户不存在");
        }

        if (!isAdmin) {
            Set<Long> scopedDeptIds = getScopedDeptIds(currentUserId);
            if (dbUser.getDeptId() == null || !scopedDeptIds.contains(dbUser.getDeptId())) {
                return R.fail(403, "超出当前权限范围");
            }
            if (sysUserService.isAdmin(dbUser.getId())) {
                return R.fail(403, "不能编辑管理员用户");
            }
            if (user.getDeptId() != null && !Objects.equals(user.getDeptId(), dbUser.getDeptId())) {
                return R.fail(403, "仅管理员可修改用户所属部门");
            }
        }

        SysUser updateEntity = new SysUser();
        updateEntity.setId(user.getId());
        updateEntity.setRealName(user.getRealName());
        updateEntity.setPhone(user.getPhone());
        if (isAdmin) {
            updateEntity.setDeptId(user.getDeptId());
        }
        if (user.getStatus() != null) {
            updateEntity.setStatus(user.getStatus());
        }
        if (StrUtil.isNotBlank(user.getPassword())) {
            updateEntity.setPassword(SmUtil.sm3(user.getPassword().trim() + dbUser.getUsername().trim()));
        }

        sysUserService.updateById(updateEntity);
        return R.ok("用户更新成功");
    }

    @ApiOperation("更新用户状态")
    @PutMapping("/status")
    public R<String> updateStatus(@RequestBody Map<String, Object> params) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = sysUserService.isAdmin(currentUserId);
        boolean isDeptLeader = sysUserService.isDeptLeader(currentUserId);
        if (!isAdmin && !isDeptLeader) {
            return R.fail(403, "无权限操作");
        }

        Long id = params.get("id") == null ? null : Long.parseLong(params.get("id").toString());
        Integer status = params.get("status") == null ? null : Integer.parseInt(params.get("status").toString());
        if (id == null || status == null) {
            return R.fail("参数不合法");
        }

        if (!isAdmin) {
            SysUser target = sysUserService.getById(id);
            Set<Long> scopedDeptIds = getScopedDeptIds(currentUserId);
            if (target == null || target.getDeptId() == null || !scopedDeptIds.contains(target.getDeptId())) {
                return R.fail(403, "超出当前权限范围");
            }
            if (sysUserService.isAdmin(target.getId())) {
                return R.fail(403, "不能修改管理员用户状态");
            }
        }

        SysUser updateEntity = new SysUser();
        updateEntity.setId(id);
        updateEntity.setStatus(status);
        sysUserService.updateById(updateEntity);
        return R.ok("状态更新成功");
    }

    @ApiOperation("获取用户角色ID")
    @GetMapping("/{id}/roles")
    public R<List<Long>> getUserRoles(@PathVariable Long id) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = sysUserService.isAdmin(currentUserId);
        boolean isDeptLeader = sysUserService.isDeptLeader(currentUserId);
        if (!isAdmin && !isDeptLeader) {
            return R.fail(403, "无权限操作");
        }

        if (!isAdmin) {
            SysUser target = sysUserService.getById(id);
            Set<Long> scopedDeptIds = getScopedDeptIds(currentUserId);
            if (target == null || target.getDeptId() == null || !scopedDeptIds.contains(target.getDeptId())) {
                return R.fail(403, "超出当前权限范围");
            }
            if (sysUserService.isAdmin(target.getId())) {
                return R.fail(403, "不能查看管理员用户角色");
            }
        }

        return R.ok(sysUserService.getRoleIds(id));
    }

    @ApiOperation("设置用户角色")
    @PutMapping("/roles")
    public R<String> setUserRoles(@RequestBody Map<String, Object> params) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!sysUserService.isAdmin(currentUserId)) {
            return R.fail(403, "仅管理员可操作");
        }

        Long userId = params.get("userId") == null ? null : Long.parseLong(params.get("userId").toString());
        if (userId == null) {
            return R.fail("用户ID不能为空");
        }

        List<Long> roleIds = new ArrayList<>();
        Object roleIdsObj = params.get("roleIds");
        if (roleIdsObj instanceof List) {
            for (Object item : (List<?>) roleIdsObj) {
                if (item != null) {
                    roleIds.add(Long.parseLong(item.toString()));
                }
            }
        }

        sysUserService.assignRoles(userId, roleIds);
        return R.ok("角色设置成功");
    }

    private void fillDeptName(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Set<Long> deptIds = users.stream().map(SysUser::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, String> deptMap = sysDeptService.listByIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        for (SysUser item : users) {
            item.setDeptName(deptMap.get(item.getDeptId()));
        }
    }

    private void fillRoleNames(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Map<Long, List<Long>> userRoleIdsMap = new HashMap<>();
        Set<Long> allRoleIds = new HashSet<>();
        for (SysUser user : users) {
            List<Long> roleIds = sysUserService.getRoleIds(user.getId());
            userRoleIdsMap.put(user.getId(), roleIds);
            allRoleIds.addAll(roleIds);
        }
        if (allRoleIds.isEmpty()) {
            for (SysUser user : users) {
                user.setRoleNames("user");
            }
            return;
        }
        Map<Long, String> roleNameMap = sysRoleService.listByIds(allRoleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName, (a, b) -> a));
        for (SysUser user : users) {
            List<Long> roleIds = userRoleIdsMap.getOrDefault(user.getId(), new ArrayList<>());
            if (roleIds.isEmpty()) {
                user.setRoleNames("user");
                continue;
            }
            String names = roleIds.stream()
                    .map(roleNameMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            user.setRoleNames(StrUtil.isBlank(names) ? "user" : names);
        }
    }

    private Set<Long> getScopedDeptIds(Long currentUserId) {
        SysUser currentUser = sysUserService.getById(currentUserId);
        if (currentUser == null || currentUser.getDeptId() == null) {
            return new HashSet<>();
        }

        List<SysDept> deptList = sysDeptService.list(new LambdaQueryWrapper<SysDept>()
                .select(SysDept::getId, SysDept::getParentId));
        if (deptList == null || deptList.isEmpty()) {
            return new HashSet<>(Collections.singletonList(currentUser.getDeptId()));
        }

        Map<Long, List<Long>> childMap = new HashMap<>();
        for (SysDept dept : deptList) {
            Long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
            childMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(dept.getId());
        }

        Set<Long> scopedDeptIds = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.offer(currentUser.getDeptId());
        while (!queue.isEmpty()) {
            Long deptId = queue.poll();
            if (deptId == null || !scopedDeptIds.add(deptId)) {
                continue;
            }
            List<Long> children = childMap.get(deptId);
            if (children != null) {
                queue.addAll(children);
            }
        }
        return scopedDeptIds;
    }
}
