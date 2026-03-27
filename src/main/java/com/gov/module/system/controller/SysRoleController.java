package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.common.result.R;
import com.gov.module.system.entity.SysRole;
import com.gov.module.system.service.SysRoleService;
import com.gov.module.system.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Api(tags = "角色管理")
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysUserService sysUserService;

    @ApiOperation("角色分页查询")
    @GetMapping("/page")
    public R<IPage<SysRole>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName
    ) {
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StrUtil.isNotBlank(roleName), SysRole::getRoleName, roleName);
        wrapper.orderByDesc(SysRole::getCreateTime);
        return R.ok(sysRoleService.page(new Page<>(pageNum, pageSize), wrapper));
    }

    @ApiOperation("角色列表")
    @GetMapping("/all")
    public R<List<SysRole>> all() {
        return R.ok(sysRoleService.list(new LambdaQueryWrapper<SysRole>()
                .select(SysRole::getId, SysRole::getRoleName, SysRole::getRoleCode, SysRole::getMenuPerms)
                .orderByAsc(SysRole::getId)));
    }

    @ApiOperation("菜单权限目录")
    @GetMapping("/menu-catalog")
    public R<List<Map<String, Object>>> menuCatalog() {
        List<Map<String, Object>> data = new ArrayList<>();
        data.add(menuItem("dashboard:view", "首页"));
        data.add(menuItem("project:manage", "项目管理"));
        data.add(menuItem("project:engineering", "工程进度"));
        data.add(menuItem("system:user", "用户管理"));
        data.add(menuItem("system:dept", "部门管理"));
        data.add(menuItem("system:role", "角色管理"));
        return R.ok(data);
    }

    @ApiOperation("新增角色")
    @PostMapping("/add")
    public R<String> add(@RequestBody SysRole role) {
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        if (StrUtil.isBlank(role.getRoleName())) {
            return R.fail("角色名称不能为空");
        }

        String roleName = role.getRoleName().trim();
        String roleCode = generateRoleCode(roleName);
        boolean exists = sysRoleService.count(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode)) > 0;
        if (exists) {
            roleCode = roleCode + "_" + System.currentTimeMillis();
        }

        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setMenuPerms(normalizeMenuPerms(role.getMenuPerms()));
        sysRoleService.save(role);
        return R.ok("角色创建成功");
    }

    @ApiOperation("更新角色")
    @PutMapping("/update")
    public R<String> update(@RequestBody SysRole role) {
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        if (role.getId() == null) {
            return R.fail("角色ID不能为空");
        }
        if (StrUtil.isBlank(role.getRoleName())) {
            return R.fail("角色名称不能为空");
        }

        SysRole dbRole = sysRoleService.getById(role.getId());
        if (dbRole == null) {
            return R.fail("角色不存在");
        }

        dbRole.setRoleName(role.getRoleName().trim());
        dbRole.setMenuPerms(normalizeMenuPerms(role.getMenuPerms()));
        sysRoleService.updateById(dbRole);
        return R.ok("角色更新成功");
    }

    @ApiOperation("更新角色菜单权限")
    @PutMapping("/{id}/menus")
    public R<String> updateMenus(@PathVariable Long id, @RequestBody Map<String, Object> payload) {
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        SysRole dbRole = sysRoleService.getById(id);
        if (dbRole == null) {
            return R.fail("角色不存在");
        }

        String menuPerms;
        Object menusObj = payload.get("menuKeys");
        if (menusObj instanceof List) {
            List<String> menus = new ArrayList<>();
            for (Object item : (List<?>) menusObj) {
                if (item != null) {
                    menus.add(String.valueOf(item));
                }
            }
            menuPerms = String.join(",", menus);
        } else {
            menuPerms = payload.get("menuPerms") == null ? null : String.valueOf(payload.get("menuPerms"));
        }

        dbRole.setMenuPerms(normalizeMenuPerms(menuPerms));
        sysRoleService.updateById(dbRole);
        return R.ok("角色菜单权限更新成功");
    }

    @ApiOperation("删除角色")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        sysRoleService.removeById(id);
        return R.ok("角色删除成功");
    }

    private Map<String, Object> menuItem(String key, String label) {
        Map<String, Object> item = new HashMap<>();
        item.put("key", key);
        item.put("label", label);
        return item;
    }

    private String generateRoleCode(String roleName) {
        String base = roleName.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
        if (StrUtil.isBlank(base)) {
            base = "role_" + System.currentTimeMillis();
        }
        if (!base.startsWith("role_")) {
            base = "role_" + base;
        }
        return base;
    }

    private String normalizeMenuPerms(String menuPerms) {
        if (StrUtil.isBlank(menuPerms)) {
            return null;
        }
        LinkedHashSet<String> keys = Arrays.stream(menuPerms.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        return String.join(",", keys);
    }
}
