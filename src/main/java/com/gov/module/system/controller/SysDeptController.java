package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.common.result.R;
import com.gov.module.system.dto.DeptCreateDTO;
import com.gov.module.system.dto.DeptUpdateDTO;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.SysDeptTreeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * 职责：提供部门树查询以及部门增删改接口。
 * 为什么存在：部门树既承担权限数据范围，又承担用户归属与审批负责人链路，需要统一收口。
 * 关键输入输出：输入为部门 DTO 或当前登录用户身份，输出为部门树 VO 或中文操作结果。
 * 关联链路：部门管理页、用户管理页、审批负责人推导、数据范围裁剪。
 */
@Api(tags = "部门管理")
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    private static final Logger perfLog = LoggerFactory.getLogger("com.gov.perf");

    @Autowired
    private SysDeptService sysDeptService;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 职责：返回当前用户可见的部门树。
     * 为什么存在：管理员需要看全量，部门负责人只看本部门及下级，普通用户不应访问。
     * 关键输入输出：输入为当前登录人身份，输出为部门树 VO 列表。
     * 关联链路：部门管理页、用户编辑页部门选择、权限数据范围。
     */
    @ApiOperation("部门树")
    @GetMapping("/tree")
    public R<List<SysDeptTreeVO>> tree() {
        long startAt = System.currentTimeMillis();
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = sysUserService.isAdmin(currentUserId);
        boolean isDeptLeader = sysUserService.isDeptLeader(currentUserId);
        if (!isAdmin && !isDeptLeader) {
            return R.fail(403, "无权限查看部门信息");
        }

        List<SysDept> deptList = sysDeptService.list(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getId));
        if (deptList.isEmpty()) {
            perfLog.info("action=deptTree userId={} scope=empty count=0 durationMs={}",
                    currentUserId, System.currentTimeMillis() - startAt);
            return R.ok(new ArrayList<SysDeptTreeVO>());
        }

        Long scopeRootId = null;
        if (!isAdmin) {
            SysUser currentUser = sysUserService.getById(currentUserId);
            if (currentUser == null || currentUser.getDeptId() == null) {
                return R.fail(403, "当前用户未绑定部门");
            }
            scopeRootId = currentUser.getDeptId();
            Set<Long> scopedDeptIds = getScopedDeptIds(scopeRootId, deptList);
            deptList = deptList.stream().filter(item -> scopedDeptIds.contains(item.getId())).collect(Collectors.toList());
        }

        Map<Long, String> leaderMap = buildLeaderMap(deptList);
        List<SysDeptTreeVO> voList = deptList.stream().map(item -> {
            SysDeptTreeVO vo = BeanUtil.copyProperties(item, SysDeptTreeVO.class);
            vo.setLeaderName(leaderMap.get(item.getLeaderId()));
            return vo;
        }).collect(Collectors.toList());

        Map<Long, SysDeptTreeVO> idMap = voList.stream()
                .collect(Collectors.toMap(SysDeptTreeVO::getId, item -> item, (a, b) -> a));

        List<SysDeptTreeVO> rootList = new ArrayList<SysDeptTreeVO>();
        for (SysDeptTreeVO vo : voList) {
            Long parentId = vo.getParentId();
            if (parentId == null || parentId == 0 || !idMap.containsKey(parentId)) {
                rootList.add(vo);
            } else {
                idMap.get(parentId).getChildren().add(vo);
            }
        }

        List<SysDeptTreeVO> result = rootList;
        if (scopeRootId != null) {
            result = new ArrayList<SysDeptTreeVO>();
            for (SysDeptTreeVO root : rootList) {
                if (Objects.equals(root.getId(), scopeRootId)) {
                    result.add(root);
                    break;
                }
            }
        }

        perfLog.info("action=deptTree userId={} scopeRootId={} count={} durationMs={}",
                currentUserId, scopeRootId, result.size(), System.currentTimeMillis() - startAt);
        return R.ok(result);
    }

    /**
     * 职责：新增部门。
     * 为什么存在：统一校验上级部门、负责人信息与管理员权限边界。
     * 关键输入输出：输入为部门创建 DTO，输出为中文创建结果。
     * 关联链路：部门管理新增弹窗。
     */
    @ApiOperation("新增部门")
    @PostMapping("/add")
    public R<String> add(@RequestBody DeptCreateDTO payload) {
        long startAt = System.currentTimeMillis();
        SysDept dept = toCreateDeptEntity(payload);
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可新增部门");
        }
        if (StrUtil.isBlank(dept.getDeptName())) {
            return R.fail("部门名称不能为空");
        }
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        sysDeptService.save(dept);
        perfLog.info("action=deptAdd operatorUserId={} deptId={} parentId={} durationMs={}",
                StpUtil.getLoginIdAsLong(), dept.getId(), dept.getParentId(), System.currentTimeMillis() - startAt);
        return R.ok("部门创建成功");
    }

    /**
     * 职责：更新部门。
     * 为什么存在：部门树关系和负责人信息会影响审批链路，需要统一校验。
     * 关键输入输出：输入为部门更新 DTO，输出为中文更新结果。
     * 关联链路：部门管理编辑弹窗。
     */
    @ApiOperation("更新部门")
    @PutMapping("/update")
    public R<String> update(@RequestBody DeptUpdateDTO payload) {
        long startAt = System.currentTimeMillis();
        SysDept dept = toUpdateDeptEntity(payload);
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可更新部门");
        }
        if (dept.getId() == null) {
            return R.fail("部门ID不能为空");
        }
        if (StrUtil.isBlank(dept.getDeptName())) {
            return R.fail("部门名称不能为空");
        }
        if (Objects.equals(dept.getId(), dept.getParentId())) {
            return R.fail("上级部门不能为自身");
        }
        sysDeptService.updateById(dept);
        perfLog.info("action=deptUpdate operatorUserId={} deptId={} parentId={} durationMs={}",
                StpUtil.getLoginIdAsLong(), dept.getId(), dept.getParentId(), System.currentTimeMillis() - startAt);
        return R.ok("部门更新成功");
    }

    /**
     * 职责：删除部门。
     * 为什么存在：删除前必须确保没有子部门和部门用户，避免破坏组织结构。
     * 关键输入输出：输入为部门 ID，输出为中文删除结果。
     * 关联链路：部门管理列表页删除动作。
     */
    @ApiOperation("删除部门")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        long startAt = System.currentTimeMillis();
        if (!sysUserService.isAdmin(StpUtil.getLoginIdAsLong())) {
            return R.fail(403, "仅管理员可删除部门");
        }

        boolean hasChild = sysDeptService.count(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, id)) > 0;
        if (hasChild) {
            return R.fail("请先删除子部门");
        }

        boolean hasUser = sysUserService.count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id)) > 0;
        if (hasUser) {
            return R.fail("部门下仍有用户，无法删除");
        }

        sysDeptService.removeById(id);
        perfLog.info("action=deptDelete operatorUserId={} deptId={} durationMs={}",
                StpUtil.getLoginIdAsLong(), id, System.currentTimeMillis() - startAt);
        return R.ok("部门删除成功");
    }

    private SysDept toCreateDeptEntity(DeptCreateDTO payload) {
        SysDept dept = new SysDept();
        if (payload == null) {
            return dept;
        }
        dept.setParentId(payload.getParentId());
        dept.setDeptName(payload.getDeptName());
        dept.setLeaderId(payload.getLeaderId());
        return dept;
    }

    private SysDept toUpdateDeptEntity(DeptUpdateDTO payload) {
        SysDept dept = new SysDept();
        if (payload == null) {
            return dept;
        }
        dept.setId(payload.getId());
        dept.setParentId(payload.getParentId());
        dept.setDeptName(payload.getDeptName());
        dept.setLeaderId(payload.getLeaderId());
        return dept;
    }

    private Set<Long> getScopedDeptIds(Long rootDeptId, List<SysDept> allDeptList) {
        Map<Long, List<Long>> childMap = new HashMap<Long, List<Long>>();
        for (SysDept dept : allDeptList) {
            Long parentId = dept.getParentId() == null ? 0L : dept.getParentId();
            childMap.computeIfAbsent(parentId, key -> new ArrayList<Long>()).add(dept.getId());
        }

        Set<Long> scopedDeptIds = new HashSet<Long>();
        Deque<Long> queue = new ArrayDeque<Long>();
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

    private Map<Long, String> buildLeaderMap(List<SysDept> deptList) {
        Set<Long> userIds = deptList.stream().map(SysDept::getLeaderId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return new HashMap<Long, String>();
        }
        return sysUserService.listByIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, item -> {
            if (StrUtil.isNotBlank(item.getRealName())) {
                return item.getRealName();
            }
            return item.getUsername();
        }, (a, b) -> a));
    }
}
