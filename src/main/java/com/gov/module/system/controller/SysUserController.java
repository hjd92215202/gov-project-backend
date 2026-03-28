package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.common.result.R;
import com.gov.module.system.dto.UserCreateDTO;
import com.gov.module.system.dto.UserRoleAssignDTO;
import com.gov.module.system.dto.UserStatusUpdateDTO;
import com.gov.module.system.dto.UserUpdateDTO;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysRole;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysRoleService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.UserAccessContext;
import com.gov.module.system.vo.UserPageVO;
import com.gov.module.system.vo.UserSimpleVO;
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

/**
 * 用户管理接口控制器。
 * 这个类负责用户分页、简表、创建、编辑、状态切换和角色分配，
 * 同时在接口层统一收敛管理员与部门负责人的数据范围规则。
 */
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

    /**
     * 用户分页查询。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param username 用户名筛选
     * @param realName 姓名筛选
     * @param status 状态筛选
     * @param deptId 部门筛选
     * @return 用户分页结果
     */
    @ApiOperation("用户分页查询")
    @GetMapping("/page")
    public R<IPage<UserPageVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId
    ) {
        UserAccessContext accessContext = currentAccessContext();
        if (!accessContext.isAdmin() && !accessContext.isDeptLeader()) {
            return R.fail(403, "无权限查看用户列表");
        }

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getId, SysUser::getDeptId, SysUser::getUsername,
                SysUser::getRealName, SysUser::getPhone, SysUser::getStatus, SysUser::getCreateTime);
        wrapper.like(StrUtil.isNotBlank(username), SysUser::getUsername, username);
        wrapper.like(StrUtil.isNotBlank(realName), SysUser::getRealName, realName);
        wrapper.eq(status != null, SysUser::getStatus, status);

        if (accessContext.isAdmin()) {
            wrapper.eq(deptId != null, SysUser::getDeptId, deptId);
        } else {
            Set<Long> scopedDeptIds = getScopedDeptIds(accessContext.getDeptId());
            if (scopedDeptIds.isEmpty()) {
                return R.fail(403, "当前用户未绑定部门");
            }
            wrapper.in(SysUser::getDeptId, scopedDeptIds);
        }

        wrapper.orderByDesc(SysUser::getCreateTime);
        IPage<SysUser> page = sysUserService.page(new Page<>(pageNum, pageSize), wrapper);
        fillDeptName(page.getRecords());
        fillRoleNames(page.getRecords());
        Page<UserPageVO> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<UserPageVO> records = new ArrayList<>();
        for (SysUser user : page.getRecords()) {
            records.add(toUserPageVO(user));
        }
        responsePage.setRecords(records);
        return R.ok(responsePage);
    }

    /**
     * 获取简版用户列表。
     * 主要供项目负责人选择、部门负责人选择等轻量场景使用。
     *
     * @return 简版用户列表
     */
    @ApiOperation("简版用户列表")
    @GetMapping("/simple")
    public R<List<UserSimpleVO>> simple() {
        UserAccessContext accessContext = currentAccessContext();

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getId, SysUser::getDeptId, SysUser::getUsername,
                SysUser::getRealName, SysUser::getPhone, SysUser::getStatus);
        wrapper.eq(SysUser::getStatus, 1);

        if (!accessContext.isAdmin() && accessContext.isDeptLeader()) {
            Set<Long> scopedDeptIds = getScopedDeptIds(accessContext.getDeptId());
            if (scopedDeptIds.isEmpty()) {
                return R.fail(403, "当前用户未绑定部门");
            }
            wrapper.in(SysUser::getDeptId, scopedDeptIds);
        } else if (!accessContext.isAdmin()) {
            wrapper.eq(SysUser::getId, accessContext.getUserId());
        }

        wrapper.orderByAsc(SysUser::getId);
        List<UserSimpleVO> result = new ArrayList<>();
        for (SysUser user : sysUserService.list(wrapper)) {
            result.add(toUserSimpleVO(user));
        }
        return R.ok(result);
    }

    /**
     * 新增用户。
     *
     * @param payload 用户创建 DTO
     * @return 创建结果
     */
    @ApiOperation("新增用户")
    @PostMapping("/add")
    public R<Map<String, Object>> add(@RequestBody UserCreateDTO payload) {
        SysUser user = toCreateUserEntity(payload);
        UserAccessContext accessContext = currentAccessContext();
        if (!accessContext.isAdmin() && !accessContext.isDeptLeader()) {
            return R.fail(403, "无权限新增用户");
        }
        if (StrUtil.isBlank(user.getUsername())) {
            return R.fail("用户名不能为空");
        }

        String username = user.getUsername().trim();
        boolean exists = sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)) > 0;
        if (exists) {
            return R.fail("用户名已存在");
        }

        if (accessContext.isDeptLeader() && !accessContext.isAdmin()) {
            Set<Long> scopedDeptIds = getScopedDeptIds(accessContext.getDeptId());
            if (user.getDeptId() == null) {
                user.setDeptId(accessContext.getDeptId());
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
        if (accessContext.isAdmin() && user.getRoleIds() != null) {
            sysUserService.assignRoles(user.getId(), user.getRoleIds());
        }

        Map<String, Object> data = new HashMap<>();
        data.put("userId", user.getId());
        return R.ok(data, "用户创建成功");
    }

    /**
     * 更新用户。
     *
     * @param payload 用户更新 DTO
     * @return 更新结果
     */
    @ApiOperation("更新用户")
    @PutMapping("/update")
    public R<String> update(@RequestBody UserUpdateDTO payload) {
        SysUser user = toUpdateUserEntity(payload);
        UserAccessContext accessContext = currentAccessContext();
        if (!accessContext.isAdmin() && !accessContext.isDeptLeader()) {
            return R.fail(403, "无权限更新用户");
        }
        if (user.getId() == null) {
            return R.fail("用户ID不能为空");
        }

        SysUser dbUser = sysUserService.getById(user.getId());
        if (dbUser == null) {
            return R.fail("用户不存在");
        }

        if (!accessContext.isAdmin()) {
            Set<Long> scopedDeptIds = getScopedDeptIds(accessContext.getDeptId());
            if (dbUser.getDeptId() == null || !scopedDeptIds.contains(dbUser.getDeptId())) {
                return R.fail(403, "超出当前权限范围");
            }
            if (sysUserService.isAdmin(dbUser.getId())) {
                return R.fail(403, "不能编辑管理员账号");
            }
            if (user.getDeptId() != null && !Objects.equals(user.getDeptId(), dbUser.getDeptId())) {
                return R.fail(403, "仅管理员可修改用户所属部门");
            }
        }

        SysUser updateEntity = new SysUser();
        updateEntity.setId(user.getId());
        updateEntity.setRealName(user.getRealName());
        updateEntity.setPhone(user.getPhone());
        if (accessContext.isAdmin()) {
            updateEntity.setDeptId(user.getDeptId());
        }
        if (user.getStatus() != null) {
            updateEntity.setStatus(user.getStatus());
        }
        if (StrUtil.isNotBlank(user.getPassword())) {
            updateEntity.setPassword(SmUtil.sm3(user.getPassword().trim() + dbUser.getUsername().trim()));
        }

        sysUserService.updateById(updateEntity);
        if (accessContext.isAdmin() && user.getRoleIds() != null) {
            sysUserService.assignRoles(user.getId(), user.getRoleIds());
        }
        return R.ok("用户更新成功");
    }

    /**
     * 更新用户启停状态。
     *
     * @param payload 状态更新 DTO
     * @return 更新结果
     */
    @ApiOperation("更新用户状态")
    @PutMapping("/status")
    public R<String> updateStatus(@RequestBody UserStatusUpdateDTO payload) {
        UserAccessContext accessContext = currentAccessContext();
        if (!accessContext.isAdmin() && !accessContext.isDeptLeader()) {
            return R.fail(403, "无权限修改用户状态");
        }

        Long id = payload == null ? null : payload.getId();
        Integer status = payload == null ? null : payload.getStatus();
        if (id == null || status == null) {
            return R.fail("用户ID和状态不能为空");
        }

        if (!accessContext.isAdmin()) {
            SysUser target = sysUserService.getById(id);
            Set<Long> scopedDeptIds = getScopedDeptIds(accessContext.getDeptId());
            if (target == null || target.getDeptId() == null || !scopedDeptIds.contains(target.getDeptId())) {
                return R.fail(403, "超出当前权限范围");
            }
            if (sysUserService.isAdmin(target.getId())) {
                return R.fail(403, "不能修改管理员账号状态");
            }
        }

        SysUser updateEntity = new SysUser();
        updateEntity.setId(id);
        updateEntity.setStatus(status);
        sysUserService.updateById(updateEntity);
        return R.ok("用户状态更新成功");
    }

    /**
     * 获取指定用户的角色 ID 列表。
     *
     * @param id 用户 ID
     * @return 角色 ID 列表
     */
    @ApiOperation("获取用户角色ID")
    @GetMapping("/{id}/roles")
    public R<List<Long>> getUserRoles(@PathVariable Long id) {
        UserAccessContext accessContext = currentAccessContext();
        if (!accessContext.isAdmin() && !accessContext.isDeptLeader()) {
            return R.fail(403, "无权限查看用户角色");
        }

        if (!accessContext.isAdmin()) {
            SysUser target = sysUserService.getById(id);
            Set<Long> scopedDeptIds = getScopedDeptIds(accessContext.getDeptId());
            if (target == null || target.getDeptId() == null || !scopedDeptIds.contains(target.getDeptId())) {
                return R.fail(403, "超出当前权限范围");
            }
            if (sysUserService.isAdmin(target.getId())) {
                return R.fail(403, "不能查看管理员账号角色");
            }
        }

        return R.ok(sysUserService.getRoleIds(id));
    }

    /**
     * 为指定用户分配角色。
     *
     * @param payload 用户角色分配 DTO
     * @return 分配结果
     */
    @ApiOperation("设置用户角色")
    @PutMapping("/roles")
    public R<String> setUserRoles(@RequestBody UserRoleAssignDTO payload) {
        UserAccessContext accessContext = currentAccessContext();
        if (!accessContext.isAdmin()) {
            return R.fail(403, "仅管理员可分配用户角色");
        }

        Long userId = payload == null ? null : payload.getUserId();
        if (userId == null) {
            return R.fail("用户ID不能为空");
        }

        List<Long> roleIds = new ArrayList<>();
        if (payload != null && payload.getRoleIds() != null) {
            for (Long item : payload.getRoleIds()) {
                if (item != null) {
                    roleIds.add(item);
                }
            }
        }

        sysUserService.assignRoles(userId, roleIds);
        return R.ok("角色设置成功");
    }

    /**
     * 把创建 DTO 转为用户实体。
     *
     * @param payload 创建 DTO
     * @return 用户实体
     */
    private SysUser toCreateUserEntity(UserCreateDTO payload) {
        SysUser user = new SysUser();
        if (payload == null) {
            return user;
        }
        user.setUsername(payload.getUsername());
        user.setPassword(payload.getPassword());
        user.setRealName(payload.getRealName());
        user.setDeptId(payload.getDeptId());
        user.setPhone(payload.getPhone());
        user.setStatus(payload.getStatus());
        user.setRoleIds(payload.getRoleIds());
        return user;
    }

    /**
     * 把更新 DTO 转为用户实体。
     *
     * @param payload 更新 DTO
     * @return 用户实体
     */
    private SysUser toUpdateUserEntity(UserUpdateDTO payload) {
        SysUser user = new SysUser();
        if (payload == null) {
            return user;
        }
        user.setId(payload.getId());
        user.setRealName(payload.getRealName());
        user.setDeptId(payload.getDeptId());
        user.setPhone(payload.getPhone());
        user.setStatus(payload.getStatus());
        user.setPassword(payload.getPassword());
        user.setRoleIds(payload.getRoleIds());
        return user;
    }

    /**
     * 批量补齐用户所属部门名称。
     *
     * @param users 用户列表
     */
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

    /**
     * 批量补齐用户角色显示名称。
     *
     * @param users 用户列表
     */
    private void fillRoleNames(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }

        List<Long> userIds = users.stream().map(SysUser::getId).filter(Objects::nonNull).collect(Collectors.toList());
        Map<Long, List<Long>> userRoleIdsMap = sysUserService.getRoleIdsMap(userIds);
        Set<Long> allRoleIds = userRoleIdsMap.values().stream()
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        String adminRoleName = getAdminRoleName();

        if (allRoleIds.isEmpty()) {
            for (SysUser user : users) {
                user.setRoleNames(resolveFallbackRoleNames(user, adminRoleName));
            }
            return;
        }

        Map<Long, String> roleNameMap = sysRoleService.listByIds(allRoleIds).stream()
                .collect(Collectors.toMap(SysRole::getId, SysRole::getRoleName, (a, b) -> a));
        for (SysUser user : users) {
            List<Long> roleIds = userRoleIdsMap.getOrDefault(user.getId(), new ArrayList<>());
            if (roleIds.isEmpty()) {
                user.setRoleNames(resolveFallbackRoleNames(user, adminRoleName));
                continue;
            }
            String names = roleIds.stream()
                    .map(roleNameMap::get)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(", "));
            user.setRoleNames(StrUtil.isBlank(names) ? "user" : names);
        }
    }

    /**
     * 获取管理员角色名称，用于兜底显示。
     *
     * @return 管理员角色名称
     */
    private String getAdminRoleName() {
        SysRole adminRole = sysRoleService.getOne(new LambdaQueryWrapper<SysRole>()
                .select(SysRole::getRoleName)
                .eq(SysRole::getRoleCode, "admin")
                .last("limit 1"));
        if (adminRole == null || StrUtil.isBlank(adminRole.getRoleName())) {
            return "admin";
        }
        return adminRole.getRoleName();
    }

    /**
     * 当用户没有显式角色记录时，返回兜底角色名称。
     *
     * @param user 用户实体
     * @param adminRoleName 管理员角色名称
     * @return 兜底角色名
     */
    private String resolveFallbackRoleNames(SysUser user, String adminRoleName) {
        if (user != null && Long.valueOf(1L).equals(user.getId())) {
            return adminRoleName;
        }
        return "user";
    }

    /**
     * 把用户实体转换为分页 VO。
     *
     * @param user 用户实体
     * @return 分页 VO
     */
    private UserPageVO toUserPageVO(SysUser user) {
        UserPageVO vo = new UserPageVO();
        if (user == null) {
            return vo;
        }
        vo.setId(user.getId());
        vo.setDeptId(user.getDeptId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setDeptName(user.getDeptName());
        vo.setRoleNames(user.getRoleNames());
        return vo;
    }

    /**
     * 把用户实体转换为简版 VO。
     *
     * @param user 用户实体
     * @return 简版 VO
     */
    private UserSimpleVO toUserSimpleVO(SysUser user) {
        UserSimpleVO vo = new UserSimpleVO();
        if (user == null) {
            return vo;
        }
        vo.setId(user.getId());
        vo.setDeptId(user.getDeptId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setStatus(user.getStatus());
        return vo;
    }

    /**
     * 计算部门负责人可见的部门范围。
     *
     * @param rootDeptId 根部门 ID
     * @return 部门范围集合
     */
    private Set<Long> getScopedDeptIds(Long rootDeptId) {
        if (rootDeptId == null) {
            return new HashSet<>();
        }

        List<SysDept> deptList = sysDeptService.list(new LambdaQueryWrapper<SysDept>()
                .select(SysDept::getId, SysDept::getParentId));
        if (deptList == null || deptList.isEmpty()) {
            return new HashSet<>(Collections.singletonList(rootDeptId));
        }

        Map<Long, List<Long>> childMap = new HashMap<>();
        for (SysDept dept : deptList) {
            Long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
            childMap.computeIfAbsent(parentId, key -> new ArrayList<>()).add(dept.getId());
        }

        Set<Long> scopedDeptIds = new HashSet<>();
        Deque<Long> queue = new ArrayDeque<>();
        queue.offer(rootDeptId);
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

    /**
     * 获取当前登录人的统一访问上下文。
     *
     * @return 当前访问上下文
     */
    private UserAccessContext currentAccessContext() {
        return sysUserService.getAccessContext(StpUtil.getLoginIdAsLong());
    }
}
