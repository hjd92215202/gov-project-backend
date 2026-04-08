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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 职责：提供角色分页、角色选项、菜单目录以及角色与菜单权限维护接口。
 * 为什么存在：把角色管理相关规则集中在一处，避免页面和服务层散落角色校验与字段转换逻辑。
 * 关键输入输出：输入为角色查询条件、角色 DTO 或菜单权限 DTO，输出为角色分页 VO、选项 VO 或中文操作结果。
 * 关联链路：系统管理 -> 角色管理、菜单权限分配、登录后菜单权限解析。
 */
@Api(tags = "角色管理")
@RestController
@RequestMapping("/system/role")
@Validated
public class SysRoleController {

    private static final Logger perfLog = LoggerFactory.getLogger("com.gov.perf");

    @Autowired
    private SysRoleService sysRoleService;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 职责：返回角色分页结果。
     * 为什么存在：角色页首屏与筛选都依赖该接口，分页层需要最小字段集和稳定排序。
     * 关键输入输出：输入为页码、分页大小和角色名；输出为角色分页 VO。
     * 关联链路：角色管理列表页。
     */
    @ApiOperation("角色分页查询")
    @GetMapping("/page")
    public R<IPage<RolePageVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String roleName
    ) {
        long startAt = System.currentTimeMillis();
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }

        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<SysRole>();
        wrapper.select(SysRole::getId, SysRole::getRoleName, SysRole::getRoleCode,
                SysRole::getMenuPerms, SysRole::getCreateTime);
        wrapper.like(StrUtil.isNotBlank(roleName), SysRole::getRoleName, roleName);
        wrapper.orderByDesc(SysRole::getCreateTime);

        IPage<SysRole> page = sysRoleService.page(new Page<SysRole>(pageNum, pageSize), wrapper);
        Page<RolePageVO> responsePage = new Page<RolePageVO>(page.getCurrent(), page.getSize(), page.getTotal());
        List<RolePageVO> records = new ArrayList<RolePageVO>();
        for (SysRole role : page.getRecords()) {
            records.add(toRolePageVO(role));
        }
        responsePage.setRecords(records);
        perfLog.info("action=rolePage pageNum={} pageSize={} total={} durationMs={}",
                pageNum, pageSize, page.getTotal(), System.currentTimeMillis() - startAt);
        return R.ok(responsePage);
    }

    /**
     * 职责：返回角色下拉选项。
     * 为什么存在：用户管理等页面只需要轻量角色选项，不需要完整分页字段。
     * 关键输入输出：输入为空，输出为角色选项 VO 列表。
     * 关联链路：用户编辑弹窗、角色相关下拉框。
     */
    @ApiOperation("角色列表")
    @GetMapping("/all")
    public R<List<RoleOptionVO>> all() {
        long startAt = System.currentTimeMillis();
        List<RoleOptionVO> result = new ArrayList<RoleOptionVO>();
        for (SysRole role : sysRoleService.list(new LambdaQueryWrapper<SysRole>()
                .select(SysRole::getId, SysRole::getRoleName, SysRole::getRoleCode, SysRole::getMenuPerms)
                .orderByAsc(SysRole::getId))) {
            result.add(toRoleOptionVO(role));
        }
        perfLog.info("action=roleAll count={} durationMs={}", result.size(), System.currentTimeMillis() - startAt);
        return R.ok(result);
    }

    /**
     * 职责：返回前端可配置的菜单目录。
     * 为什么存在：统一菜单键与中文文案来源，避免前端硬编码菜单权限目录。
     * 关键输入输出：输入为空，输出为菜单键与中文标签列表。
     * 关联链路：角色菜单权限分配弹窗。
     */
    @ApiOperation("菜单权限目录")
    @GetMapping("/menu-catalog")
    public R<List<Map<String, Object>>> menuCatalog() {
        long startAt = System.currentTimeMillis();
        List<Map<String, Object>> data = new ArrayList<Map<String, Object>>();
        data.add(menuItem("dashboard:view", "首页"));
        data.add(menuItem("project:manage", "项目管理"));
        data.add(menuItem("project:engineering", "工程进度"));
        data.add(menuItem("system:user", "用户管理"));
        data.add(menuItem("system:dept", "部门管理"));
        data.add(menuItem("system:role", "角色管理"));
        data.add(menuItem("system:audit", "审计日志"));
        data.add(menuItem("system:frontend-monitor", "前端监控"));
        perfLog.info("action=roleMenuCatalog count={} durationMs={}", data.size(), System.currentTimeMillis() - startAt);
        return R.ok(data);
    }

    /**
     * 职责：新增角色。
     * 为什么存在：统一校验角色名、角色编码生成与菜单权限规范化。
     * 关键输入输出：输入为角色创建 DTO，输出为中文创建结果。
     * 关联链路：角色管理新增弹窗。
     */
    @ApiOperation("新增角色")
    @PostMapping("/add")
    public R<String> add(@Valid @RequestBody RoleCreateDTO payload) {
        long startAt = System.currentTimeMillis();
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
        perfLog.info("action=roleAdd operatorUserId={} roleId={} roleCode={} durationMs={}",
                StpUtil.getLoginIdAsLong(), role.getId(), role.getRoleCode(), System.currentTimeMillis() - startAt);
        return R.ok("角色创建成功");
    }

    /**
     * 职责：更新角色基础信息。
     * 为什么存在：角色名称和菜单权限会影响前端菜单与权限判定，需要统一收口更新。
     * 关键输入输出：输入为角色更新 DTO，输出为中文更新结果。
     * 关联链路：角色管理编辑弹窗。
     */
    @ApiOperation("更新角色")
    @PutMapping("/update")
    public R<String> update(@Valid @RequestBody RoleUpdateDTO payload) {
        long startAt = System.currentTimeMillis();
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
        perfLog.info("action=roleUpdate operatorUserId={} roleId={} durationMs={}",
                StpUtil.getLoginIdAsLong(), dbRole.getId(), System.currentTimeMillis() - startAt);
        return R.ok("角色更新成功");
    }

    /**
     * 职责：更新角色菜单权限。
     * 为什么存在：保留兼容旧字段 `menuPerms`，同时支持新 DTO 中的 `menuKeys`。
     * 关键输入输出：输入为角色 ID 与菜单权限 DTO，输出为中文更新结果。
     * 关联链路：角色菜单权限分配弹窗。
     */
    @ApiOperation("更新角色菜单权限")
    @PutMapping("/{id}/menus")
    public R<String> updateMenus(@PathVariable Long id, @Valid @RequestBody RoleMenuUpdateDTO payload) {
        long startAt = System.currentTimeMillis();
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        SysRole dbRole = sysRoleService.getById(id);
        if (dbRole == null) {
            return R.fail("角色不存在");
        }

        String menuPerms;
        if (payload != null && payload.getMenuKeys() != null) {
            List<String> menus = new ArrayList<String>();
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
        perfLog.info("action=roleUpdateMenus operatorUserId={} roleId={} durationMs={}",
                StpUtil.getLoginIdAsLong(), id, System.currentTimeMillis() - startAt);
        return R.ok("角色菜单权限更新成功");
    }

    /**
     * 职责：删除角色。
     * 为什么存在：角色删除属于高风险权限操作，统一保留管理员校验入口。
     * 关键输入输出：输入为角色 ID，输出为中文删除结果。
     * 关联链路：角色管理列表页删除动作。
     */
    @ApiOperation("删除角色")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        long startAt = System.currentTimeMillis();
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可操作");
        }
        sysRoleService.removeById(id);
        perfLog.info("action=roleDelete operatorUserId={} roleId={} durationMs={}",
                StpUtil.getLoginIdAsLong(), id, System.currentTimeMillis() - startAt);
        return R.ok("角色删除成功");
    }

    /**
     * 职责：构造单个菜单目录项。
     * 为什么存在：集中维护菜单目录输出结构，避免手写 map 字段不一致。
     * 关键输入输出：输入为菜单 key 与中文标签，输出为目录项 map。
     * 关联链路：菜单目录接口。
     */
    private Map<String, Object> menuItem(String key, String label) {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("key", key);
        item.put("label", label);
        return item;
    }

    /**
     * 职责：根据角色名称生成角色编码。
     * 为什么存在：避免新增角色时手工输入编码导致格式不统一或与历史角色冲突。
     * 关键输入输出：输入为角色名称，输出为标准化角色编码。
     * 关联链路：角色新增。
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
     * 职责：规范化菜单权限字符串。
     * 为什么存在：去重、去空白、保留顺序，避免历史脏数据影响菜单权限解析。
     * 关键输入输出：输入为逗号分隔的菜单字符串，输出为规范化后的字符串。
     * 关联链路：角色新增、角色更新、角色菜单权限更新。
     */
    private String normalizeMenuPerms(String menuPerms) {
        if (StrUtil.isBlank(menuPerms)) {
            return null;
        }
        LinkedHashSet<String> keys = Arrays.stream(menuPerms.split(","))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(LinkedHashSet<String>::new, LinkedHashSet<String>::add, LinkedHashSet<String>::addAll);
        return String.join(",", keys);
    }

    /**
     * 职责：把角色实体转换为分页 VO。
     * 为什么存在：分页页只需要前端展示字段，避免直接暴露实体结构。
     * 关键输入输出：输入为角色实体，输出为角色分页 VO。
     * 关联链路：角色分页接口。
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
     * 职责：把角色实体转换为下拉 VO。
     * 为什么存在：角色下拉只关心轻量字段，减少页面不必要的数据负担。
     * 关键输入输出：输入为角色实体，输出为角色选项 VO。
     * 关联链路：用户编辑页、角色相关下拉框。
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
