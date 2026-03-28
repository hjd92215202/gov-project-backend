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
 * 部门管理接口控制器。
 * 该类负责部门树展示、部门增删改，以及在部门树场景下按角色裁剪可见范围。
 */
@Api(tags = "部门管理")
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    @Autowired
    private SysDeptService sysDeptService;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 获取部门树。
     *
     * @return 部门树结果
     */
    @ApiOperation("部门树")
    @GetMapping("/tree")
    public R<List<SysDeptTreeVO>> tree() {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        boolean isAdmin = sysUserService.isAdmin(currentUserId);
        boolean isDeptLeader = sysUserService.isDeptLeader(currentUserId);
        if (!isAdmin && !isDeptLeader) {
            return R.fail(403, "无权限查看部门信息");
        }

        List<SysDept> deptList = sysDeptService.list(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getId));
        if (deptList.isEmpty()) {
            return R.ok(new ArrayList<>());
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

        List<SysDeptTreeVO> rootList = new ArrayList<>();
        for (SysDeptTreeVO vo : voList) {
            Long parentId = vo.getParentId();
            if (parentId == null || parentId == 0 || !idMap.containsKey(parentId)) {
                rootList.add(vo);
            } else {
                idMap.get(parentId).getChildren().add(vo);
            }
        }

        if (scopeRootId != null) {
            for (SysDeptTreeVO root : rootList) {
                if (Objects.equals(root.getId(), scopeRootId)) {
                    return R.ok(Collections.singletonList(root));
                }
            }
            return R.ok(new ArrayList<>());
        }
        return R.ok(rootList);
    }

    /**
     * 新增部门。
     *
     * @param payload 部门创建 DTO
     * @return 创建结果
     */
    @ApiOperation("新增部门")
    @PostMapping("/add")
    public R<String> add(@RequestBody DeptCreateDTO payload) {
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
        return R.ok("部门创建成功");
    }

    /**
     * 更新部门。
     *
     * @param payload 部门更新 DTO
     * @return 更新结果
     */
    @ApiOperation("更新部门")
    @PutMapping("/update")
    public R<String> update(@RequestBody DeptUpdateDTO payload) {
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
        return R.ok("部门更新成功");
    }

    /**
     * 删除部门。
     *
     * @param id 部门 ID
     * @return 删除结果
     */
    @ApiOperation("删除部门")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
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
        return R.ok("部门删除成功");
    }

    /**
     * 把创建 DTO 转为部门实体。
     *
     * @param payload 创建 DTO
     * @return 部门实体
     */
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

    /**
     * 把更新 DTO 转为部门实体。
     *
     * @param payload 更新 DTO
     * @return 部门实体
     */
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

    /**
     * 在已有部门列表中计算指定根部门的可见范围。
     *
     * @param rootDeptId 根部门 ID
     * @param allDeptList 全量部门列表
     * @return 部门范围集合
     */
    private Set<Long> getScopedDeptIds(Long rootDeptId, List<SysDept> allDeptList) {
        Map<Long, List<Long>> childMap = new HashMap<>();
        for (SysDept dept : allDeptList) {
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
     * 批量构建部门负责人姓名映射。
     *
     * @param deptList 部门列表
     * @return 负责人姓名映射
     */
    private Map<Long, String> buildLeaderMap(List<SysDept> deptList) {
        Set<Long> userIds = deptList.stream().map(SysDept::getLeaderId).filter(Objects::nonNull).collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        return sysUserService.listByIds(userIds).stream().collect(Collectors.toMap(SysUser::getId, item -> {
            if (StrUtil.isNotBlank(item.getRealName())) {
                return item.getRealName();
            }
            return item.getUsername();
        }, (a, b) -> a));
    }
}
