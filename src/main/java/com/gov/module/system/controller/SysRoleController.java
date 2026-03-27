package com.gov.module.system.controller;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.common.result.R;
import com.gov.module.system.entity.SysRole;
import com.gov.module.system.service.SysRoleService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Api(tags = "系统角色管理")
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @ApiOperation("分页查询角色")
    @GetMapping("/page")
    public R<IPage<SysRole>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName,
            @RequestParam(required = false) String roleCode
    ) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(roleName), SysRole::getRoleName, roleName);
        wrapper.like(StrUtil.isNotBlank(roleCode), SysRole::getRoleCode, roleCode);
        wrapper.orderByDesc(SysRole::getCreateTime);
        return R.ok(sysRoleService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @ApiOperation("新增角色")
    @PostMapping("/add")
    public R<String> add(@RequestBody SysRole role) {
        if (StrUtil.isBlank(role.getRoleName()) || StrUtil.isBlank(role.getRoleCode())) {
            return R.fail("角色名称和编码不能为空");
        }
        boolean exists = sysRoleService.count(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, role.getRoleCode().trim())) > 0;
        if (exists) {
            return R.fail("角色编码已存在");
        }
        role.setRoleName(role.getRoleName().trim());
        role.setRoleCode(role.getRoleCode().trim());
        sysRoleService.save(role);
        return R.ok("新增角色成功");
    }

    @ApiOperation("更新角色")
    @PutMapping("/update")
    public R<String> update(@RequestBody SysRole role) {
        if (role.getId() == null) {
            return R.fail("角色ID不能为空");
        }
        if (StrUtil.isBlank(role.getRoleName()) || StrUtil.isBlank(role.getRoleCode())) {
            return R.fail("角色名称和编码不能为空");
        }
        boolean exists = sysRoleService.count(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, role.getRoleCode().trim())
                .ne(SysRole::getId, role.getId())) > 0;
        if (exists) {
            return R.fail("角色编码已存在");
        }
        role.setRoleName(role.getRoleName().trim());
        role.setRoleCode(role.getRoleCode().trim());
        sysRoleService.updateById(role);
        return R.ok("更新角色成功");
    }

    @ApiOperation("删除角色")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        sysRoleService.removeById(id);
        return R.ok("删除角色成功");
    }
}
