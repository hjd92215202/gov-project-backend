package com.gov.module.system.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SmUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.common.result.R;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Api(tags = "系统用户管理")
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

    @ApiOperation("分页查询用户")
    @GetMapping("/page")
    public R<IPage<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String realName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long deptId
    ) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getId, SysUser::getDeptId, SysUser::getUsername,
                SysUser::getRealName, SysUser::getPhone, SysUser::getStatus, SysUser::getCreateTime);
        wrapper.like(StrUtil.isNotBlank(username), SysUser::getUsername, username);
        wrapper.like(StrUtil.isNotBlank(realName), SysUser::getRealName, realName);
        wrapper.eq(status != null, SysUser::getStatus, status);
        wrapper.eq(deptId != null, SysUser::getDeptId, deptId);
        wrapper.orderByDesc(SysUser::getCreateTime);

        IPage<SysUser> page = sysUserService.page(new Page<>(pageNum, pageSize), wrapper);
        fillDeptName(page.getRecords());
        return R.ok(page);
    }

    @ApiOperation("用户下拉列表")
    @GetMapping("/simple")
    public R<List<SysUser>> simple() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysUser::getId, SysUser::getUsername, SysUser::getRealName, SysUser::getStatus);
        wrapper.eq(SysUser::getStatus, 1);
        wrapper.orderByAsc(SysUser::getId);
        return R.ok(sysUserService.list(wrapper));
    }

    @ApiOperation("新增用户")
    @PostMapping("/add")
    public R<String> add(@RequestBody SysUser user) {
        if (StrUtil.isBlank(user.getUsername())) {
            return R.fail("用户名不能为空");
        }
        String username = user.getUsername().trim();
        boolean exists = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)) > 0;
        if (exists) {
            return R.fail("用户名已存在");
        }

        String rawPassword = StrUtil.isBlank(user.getPassword()) ? "123456" : user.getPassword().trim();
        user.setUsername(username);
        user.setPassword(SmUtil.sm3(rawPassword + username));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        sysUserService.save(user);
        return R.ok("新增用户成功");
    }

    @ApiOperation("更新用户")
    @PutMapping("/update")
    public R<String> update(@RequestBody SysUser user) {
        if (user.getId() == null) {
            return R.fail("用户ID不能为空");
        }
        SysUser dbUser = sysUserService.getById(user.getId());
        if (dbUser == null) {
            return R.fail("用户不存在");
        }

        SysUser updateEntity = new SysUser();
        updateEntity.setId(user.getId());
        updateEntity.setRealName(user.getRealName());
        updateEntity.setPhone(user.getPhone());
        updateEntity.setDeptId(user.getDeptId());
        if (user.getStatus() != null) {
            updateEntity.setStatus(user.getStatus());
        }
        if (StrUtil.isNotBlank(user.getPassword())) {
            updateEntity.setPassword(SmUtil.sm3(user.getPassword().trim() + dbUser.getUsername().trim()));
        }
        sysUserService.updateById(updateEntity);
        return R.ok("更新用户成功");
    }

    @ApiOperation("更新用户状态")
    @PutMapping("/status")
    public R<String> updateStatus(@RequestBody Map<String, Object> params) {
        Long id = params.get("id") == null ? null : Long.parseLong(params.get("id").toString());
        Integer status = params.get("status") == null ? null : Integer.parseInt(params.get("status").toString());
        if (id == null || status == null) {
            return R.fail("参数不能为空");
        }

        SysUser updateEntity = new SysUser();
        updateEntity.setId(id);
        updateEntity.setStatus(status);
        sysUserService.updateById(updateEntity);
        return R.ok("状态更新成功");
    }

    private void fillDeptName(List<SysUser> users) {
        if (users == null || users.isEmpty()) {
            return;
        }
        Set<Long> deptIds = users.stream()
                .map(SysUser::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return;
        }
        Map<Long, String> deptMap = sysDeptService.listByIds(deptIds).stream()
                .collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
        for (SysUser user : users) {
            user.setDeptName(deptMap.get(user.getDeptId()));
        }
    }
}
