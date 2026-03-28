package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.common.result.R;
import com.gov.module.system.dto.RoleCreateDTO;
import com.gov.module.system.dto.RoleMenuUpdateDTO;
import com.gov.module.system.dto.RoleUpdateDTO;
import com.gov.module.system.entity.SysRole;
import com.gov.module.system.service.SysRoleService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.RoleOptionVO;
import com.gov.module.system.vo.RolePageVO;
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

/**
 * 角色管理接口控制器。
 * 该类负责角色分页、角色列表、菜单目录、角色增删改和菜单权限分配。
 */
@Api(tags = "角色管理")
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 角色分页查询。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param roleName 角色名称筛选
     * @return 角色分页结果
     */
    @ApiOperation("角色分页查询")
    @GetMapping("/page")
    public R<IPage<RolePageVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName
    ) {
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(SysRole::getId, SysRole::getRoleName, SysRole::getRoleCode,
                SysRole::getMenuPerms, SysRole::getCreateTime);
        wrapper.like(StrUtil.isNotBlank(roleName), SysRole::getRoleName, roleName);
        wrapper.orderByDesc(SysRole::getCreateTime);
        IPage<SysRole> page = sysRoleService.page(new Page<>(pageNum, pageSize), wrapper);
        Page<RolePageVO> responsePage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<RolePageVO> records = new ArrayList<>();
        for (SysRole role : page.getRecords()) {
            records.add(toRolePageVO(role));
        }
        responsePage.setRecords(records);
        return R.ok(responsePage);
    }

    /**
     * 获取角色下拉列表。
     *
     * @return 角色列表
     */
    @ApiOperation("角色列表")
    @GetMapping("/all")
    public R<List<RoleOptionVO>> all() {
        List<RoleOptionVO> result = new ArrayList<>();
        for (SysRole role : sysRoleService.list(new LambdaQueryWrapper<SysRole>()
                .select(SysRole::getId, SysRole::getRoleName, SysRole::getRoleCode, SysRole::getMenuPerms)
                .orderByAsc(SysRole::getId))) {
            result.add(toRoleOptionVO(role));
        }
        return R.ok(result);
    }

    /**
     * 返回前端可选择的菜单权限目录。
     *
     * @return 菜单目录列表
     */
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

    /**
     * 新增角色。
     *
     * @param payload 角色创建 DTO
     * @return 创建结果
     */
    @ApiOperation("新增角色")
    @PostMapping("/add")
    public R<String> add(@RequestBody RoleCreateDTO payload) {
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        if (payload == null || StrUtil.isBlank(payload.getRoleName())) {
            return R.fail("角色名称不能为空");
        }

        String roleName = payload.getRoleName().trim();
        String roleCode = generateRoleCode(roleName);
        boolean exists = sysRoleService.count(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleCode, roleCode)) > 0;
        if (exists) {
            roleCode = roleCode + "_" + System.currentTimeMillis();
        }

        SysRole role = new SysRole();
        role.setRoleName(roleName);
        role.setRoleCode(roleCode);
        role.setMenuPerms(normalizeMenuPerms(payload.getMenuPerms()));
        sysRoleService.save(role);
        return R.ok("角色创建成功");
    }

    /**
     * 更新角色基础信息。
     *
     * @param payload 角色更新 DTO
     * @return 更新结果
     */
    @ApiOperation("更新角色")
    @PutMapping("/update")
    public R<String> update(@RequestBody RoleUpdateDTO payload) {
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        if (payload == null || payload.getId() == null) {
            return R.fail("角色ID不能为空");
        }
        if (StrUtil.isBlank(payload.getRoleName())) {
            return R.fail("角色名称不能为空");
        }

        SysRole dbRole = sysRoleService.getById(payload.getId());
        if (dbRole == null) {
            return R.fail("角色不存在");
        }

        dbRole.setRoleName(payload.getRoleName().trim());
        dbRole.setMenuPerms(normalizeMenuPerms(payload.getMenuPerms()));
        sysRoleService.updateById(dbRole);
        return R.ok("角色更新成功");
    }

    /**
     * 更新角色菜单权限。
     *
     * @param id 角色 ID
     * @param payload 菜单权限 DTO
     * @return 更新结果
     */
    @ApiOperation("更新角色菜单权限")
    @PutMapping("/{id}/menus")
    public R<String> updateMenus(@PathVariable Long id, @RequestBody RoleMenuUpdateDTO payload) {
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        SysRole dbRole = sysRoleService.getById(id);
        if (dbRole == null) {
            return R.fail("角色不存在");
        }

        String menuPerms;
        if (payload != null && payload.getMenuKeys() != null) {
            List<String> menus = new ArrayList<>();
            for (String item : payload.getMenuKeys()) {
                if (item != null) {
                    menus.add(item);
                }
            }
            menuPerms = String.join(",", menus);
        } else {
            menuPerms = payload == null ? null : payload.getMenuPerms();
        }

        dbRole.setMenuPerms(normalizeMenuPerms(menuPerms));
        sysRoleService.updateById(dbRole);
        return R.ok("角色菜单权限更新成功");
    }

    /**
     * 删除角色。
     *
     * @param id 角色 ID
     * @return 删除结果
     */
    @ApiOperation("删除角色")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        sysRoleService.removeById(id);
        return R.ok("角色删除成功");
    }

    /**
     * 构造单条菜单目录项。
     *
     * @param key 菜单键
     * @param label 菜单显示名
     * @return 菜单项
     */
    private Map<String, Object> menuItem(String key, String label) {
        Map<String, Object> item = new HashMap<>();
        item.put("key", key);
        item.put("label", label);
        return item;
    }

    /**
     * 根据角色名称生成角色编码。
     *
     * @param roleName 角色名称
     * @return 角色编码
     */
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

    /**
     * 规范化菜单权限字符串，去重并去除空白项。
     *
     * @param menuPerms 菜单权限字符串
     * @return 规范化后的字符串
     */
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

    /**
     * 把角色实体转换为分页 VO。
     *
     * @param role 角色实体
     * @return 分页 VO
     */
    private RolePageVO toRolePageVO(SysRole role) {
        RolePageVO vo = new RolePageVO();
        if (role == null) {
            return vo;
        }
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setMenuPerms(role.getMenuPerms());
        vo.setCreateTime(role.getCreateTime());
        return vo;
    }

    /**
     * 把角色实体转换为下拉 VO。
     *
     * @param role 角色实体
     * @return 下拉 VO
     */
    private RoleOptionVO toRoleOptionVO(SysRole role) {
        RoleOptionVO vo = new RoleOptionVO();
        if (role == null) {
            return vo;
        }
        vo.setId(role.getId());
        vo.setRoleName(role.getRoleName());
        vo.setRoleCode(role.getRoleCode());
        vo.setMenuPerms(role.getMenuPerms());
        return vo;
    }
}
