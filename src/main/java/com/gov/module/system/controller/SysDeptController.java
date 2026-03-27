package com.gov.module.system.controller;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gov.common.result.R;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.SysDeptTreeVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@Api(tags = "系统部门管理")
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    @Autowired
    private SysDeptService sysDeptService;

    @Autowired
    private SysUserService sysUserService;

    @ApiOperation("部门树")
    @GetMapping("/tree")
    public R<List<SysDeptTreeVO>> tree() {
        List<SysDept> deptList = sysDeptService.list(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getId));
        if (deptList.isEmpty()) {
            return R.ok(new ArrayList<SysDeptTreeVO>());
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
        return R.ok(rootList);
    }

    @ApiOperation("新增部门")
    @PostMapping("/add")
    public R<String> add(@RequestBody SysDept dept) {
        if (StrUtil.isBlank(dept.getDeptName())) {
            return R.fail("部门名称不能为空");
        }
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        sysDeptService.save(dept);
        return R.ok("新增部门成功");
    }

    @ApiOperation("更新部门")
    @PutMapping("/update")
    public R<String> update(@RequestBody SysDept dept) {
        if (dept.getId() == null) {
            return R.fail("部门ID不能为空");
        }
        if (StrUtil.isBlank(dept.getDeptName())) {
            return R.fail("部门名称不能为空");
        }
        if (Objects.equals(dept.getId(), dept.getParentId())) {
            return R.fail("上级部门不能是自己");
        }
        sysDeptService.updateById(dept);
        return R.ok("更新部门成功");
    }

    @ApiOperation("删除部门")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        boolean hasChild = sysDeptService.count(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, id)) > 0;
        if (hasChild) {
            return R.fail("请先删除子部门");
        }
        boolean hasUser = sysUserService.count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getDeptId, id)) > 0;
        if (hasUser) {
            return R.fail("该部门下仍有用户，无法删除");
        }
        sysDeptService.removeById(id);
        return R.ok("删除部门成功");
    }

    private Map<Long, String> buildLeaderMap(List<SysDept> deptList) {
        Set<Long> userIds = deptList.stream()
                .map(SysDept::getLeaderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        return sysUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(SysUser::getId, item -> {
                    if (StrUtil.isNotBlank(item.getRealName())) {
                        return item.getRealName();
                    }
                    return item.getUsername();
                }, (a, b) -> a));
    }
}
