package com.gov.module.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.common.result.R;
import com.gov.module.flow.service.FlowService;
import com.gov.module.project.entity.BizProject;
import com.gov.module.project.service.BizProjectService;
import com.gov.module.project.vo.ProjectMapVO;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Api(tags = "项目管理")
@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private BizProjectService bizProjectService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

    @Autowired
    private FlowService flowService;

    @ApiOperation("新增项目")
    @PostMapping("/add")
    public R<String> add(@RequestBody BizProject project) {
        if (StrUtil.isBlank(project.getProjectName())) {
            return R.fail("项目名称不能为空");
        }
        if (project.getStatus() == null) {
            project.setStatus(0);
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        SysUser currentUser = sysUserService.getById(currentUserId);
        if (currentUser == null) {
            return R.fail(403, "当前登录用户不存在");
        }
        if (project.getCreatorId() == null) {
            project.setCreatorId(currentUserId);
        }
        if (project.getCreatorDeptId() == null) {
            project.setCreatorDeptId(currentUser.getDeptId());
        }
        String leaderError = fillLeaderByPermission(project, currentUser);
        if (leaderError != null) {
            return R.fail(403, leaderError);
        }

        bizProjectService.save(project);
        return R.ok("项目创建成功");
    }

    @ApiOperation("项目分页查询")
    @GetMapping("/page")
    public R<IPage<BizProject>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String projectName,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district
    ) {
        LambdaQueryWrapper<BizProject> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(projectName), BizProject::getProjectName, projectName);
        queryWrapper.eq(status != null, BizProject::getStatus, status);
        queryWrapper.eq(StrUtil.isNotBlank(province), BizProject::getProvince, province);
        queryWrapper.eq(StrUtil.isNotBlank(city), BizProject::getCity, city);
        queryWrapper.eq(StrUtil.isNotBlank(district), BizProject::getDistrict, district);

        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (!applyProjectScope(queryWrapper, currentUserId)) {
            return R.fail(403, "当前用户未绑定部门");
        }

        queryWrapper.orderByDesc(BizProject::getCreateTime);

        Page<BizProject> page = new Page<>(pageNum, pageSize);
        IPage<BizProject> result = bizProjectService.page(page, queryWrapper);
        return R.ok(result);
    }

    @ApiOperation("项目详情")
    @GetMapping("/get/{id}")
    public R<BizProject> get(@PathVariable Long id) {
        BizProject project = bizProjectService.getById(id);
        if (project == null) {
            return R.fail("项目不存在");
        }
        if (!canOperateProject(project)) {
            return R.fail(403, "无权限查看该项目");
        }
        return R.ok(project);
    }

    @ApiOperation("更新项目")
    @PutMapping("/update")
    public R<String> update(@RequestBody BizProject project) {
        if (project.getId() == null) {
            return R.fail("项目ID不能为空");
        }
        if (StrUtil.isBlank(project.getProjectName())) {
            return R.fail("项目名称不能为空");
        }

        BizProject dbProject = bizProjectService.getById(project.getId());
        if (dbProject == null) {
            return R.fail("项目不存在");
        }
        if (!isDraftStatus(dbProject.getStatus())) {
            return R.fail(403, "仅草稿状态项目可编辑");
        }
        if (!canOperateProject(dbProject)) {
            return R.fail(403, "无权限编辑该项目");
        }

        Long currentUserId = StpUtil.getLoginIdAsLong();
        SysUser currentUser = sysUserService.getById(currentUserId);
        if (currentUser == null) {
            return R.fail(403, "当前登录用户不存在");
        }
        if (dbProject.getCreatorId() == null) {
            dbProject.setCreatorId(currentUserId);
            dbProject.setCreatorDeptId(currentUser.getDeptId());
        }

        if (project.getCreatorId() == null) {
            project.setCreatorId(dbProject.getCreatorId());
        }
        if (project.getCreatorDeptId() == null) {
            project.setCreatorDeptId(dbProject.getCreatorDeptId());
        }
        project.setStatus(dbProject.getStatus());

        String leaderError = fillLeaderByPermission(project, currentUser);
        if (leaderError != null) {
            return R.fail(403, leaderError);
        }

        bizProjectService.updateById(project);
        return R.ok("项目更新成功");
    }

    @ApiOperation("删除项目")
    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable Long id) {
        BizProject dbProject = bizProjectService.getById(id);
        if (dbProject == null) {
            return R.fail("项目不存在");
        }
        if (!isDraftStatus(dbProject.getStatus())) {
            return R.fail(403, "仅草稿状态项目可删除");
        }
        if (!canOperateProject(dbProject)) {
            return R.fail(403, "无权限删除该项目");
        }

        bizProjectService.removeById(id);
        return R.ok("项目删除成功");
    }

    @ApiOperation("提交项目审批")
    @PostMapping("/submit")
    public R<String> submit(@RequestBody BizProject project) {
        if (StrUtil.isBlank(project.getProjectName())) {
            return R.fail("项目名称不能为空");
        }

        Long projectId;
        Long currentUserId = StpUtil.getLoginIdAsLong();
        SysUser loginUser = sysUserService.getById(currentUserId);

        if (project.getId() != null && bizProjectService.getById(project.getId()) != null) {
            BizProject dbProject = bizProjectService.getById(project.getId());
            if (!isDraftStatus(dbProject.getStatus())) {
                return R.fail(403, "仅草稿状态项目可提交审批");
            }
            if (!canOperateProject(dbProject)) {
                return R.fail(403, "无权限提交该项目审批");
            }

            if (project.getCreatorId() == null) {
                project.setCreatorId(dbProject.getCreatorId());
            }
            if (project.getCreatorDeptId() == null) {
                project.setCreatorDeptId(dbProject.getCreatorDeptId());
            }

            String leaderError = fillLeaderByPermission(project, loginUser);
            if (leaderError != null) {
                return R.fail(403, leaderError);
            }

            project.setStatus(1);
            bizProjectService.updateById(project);
            projectId = project.getId();
        } else {
            project.setStatus(1);
            if (project.getCreatorId() == null) {
                project.setCreatorId(currentUserId);
            }
            if (project.getCreatorDeptId() == null && loginUser != null) {
                project.setCreatorDeptId(loginUser.getDeptId());
            }

            String leaderError = fillLeaderByPermission(project, loginUser);
            if (leaderError != null) {
                return R.fail(403, leaderError);
            }

            bizProjectService.save(project);
            projectId = project.getId();
        }

        if (loginUser == null) {
            return R.fail("当前登录用户不存在");
        }
        if (loginUser.getDeptId() == null) {
            return R.fail("当前用户未绑定部门");
        }

        SysDept dept = sysDeptService.getById(loginUser.getDeptId());
        if (dept == null || dept.getLeaderId() == null) {
            return R.fail("部门负责人未配置");
        }
        if (sysUserService.getById(dept.getLeaderId()) == null) {
            return R.fail("部门负责人用户不存在");
        }

        Map<String, Object> vars = new HashMap<>();
        vars.put("currentAssignee", dept.getLeaderId().toString());
        flowService.startProcess(projectId.toString(), vars);

        return R.ok("提交审批成功");
    }

    @ApiOperation("地图点位列表")
    @GetMapping("/map/list")
    public R<List<ProjectMapVO>> getMapList(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district
    ) {
        LambdaQueryWrapper<BizProject> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(BizProject::getId, BizProject::getProjectName,
                BizProject::getAddress, BizProject::getLongitude,
                BizProject::getLatitude, BizProject::getProvince,
                BizProject::getCity, BizProject::getDistrict);

        if (StrUtil.isNotBlank(province)) {
            queryWrapper.eq(BizProject::getProvince, province);
        }
        if (StrUtil.isNotBlank(city)) {
            queryWrapper.eq(BizProject::getCity, city);
        }
        if (StrUtil.isNotBlank(district)) {
            queryWrapper.eq(BizProject::getDistrict, district);
        }
        if (!applyProjectScope(queryWrapper, StpUtil.getLoginIdAsLong())) {
            return R.ok(Collections.emptyList());
        }

        List<BizProject> list = bizProjectService.list(queryWrapper);
        List<ProjectMapVO> voList = JSONUtil.toList(JSONUtil.parseArray(list), ProjectMapVO.class);
        return R.ok(voList);
    }

    private boolean canOperateProject(BizProject project) {
        Long currentUserId = StpUtil.getLoginIdAsLong();
        if (sysUserService.isAdmin(currentUserId)) {
            return true;
        }
        SysUser currentUser = sysUserService.getById(currentUserId);
        if (currentUser == null) {
            return false;
        }
        if (sysUserService.isDeptLeader(currentUserId)) {
            return currentUser.getDeptId() != null
                    && project.getCreatorDeptId() != null
                    && Objects.equals(currentUser.getDeptId(), project.getCreatorDeptId());
        }
        return Objects.equals(project.getCreatorId(), currentUserId);
    }

    private boolean applyProjectScope(LambdaQueryWrapper<BizProject> queryWrapper, Long currentUserId) {
        if (sysUserService.isAdmin(currentUserId)) {
            return true;
        }
        SysUser currentUser = sysUserService.getById(currentUserId);
        if (currentUser == null) {
            return false;
        }
        if (sysUserService.isDeptLeader(currentUserId)) {
            if (currentUser.getDeptId() == null) {
                return false;
            }
            queryWrapper.eq(BizProject::getCreatorDeptId, currentUser.getDeptId());
            return true;
        }
        queryWrapper.eq(BizProject::getCreatorId, currentUserId);
        return true;
    }

    private String fillLeaderByPermission(BizProject project, SysUser currentUser) {
        if (currentUser == null) {
            return "当前登录用户不存在";
        }
        Long currentUserId = currentUser.getId();
        boolean isAdmin = sysUserService.isAdmin(currentUserId);
        boolean isDeptLeader = sysUserService.isDeptLeader(currentUserId);

        Long leaderUserId = project.getLeaderUserId();
        if (leaderUserId != null) {
            if (!isAdmin && !isDeptLeader && !Objects.equals(currentUserId, leaderUserId)) {
                return "普通用户只能将自己设为负责人";
            }
            SysUser leaderUser = sysUserService.getById(leaderUserId);
            if (leaderUser == null || !Objects.equals(leaderUser.getStatus(), 1)) {
                return "项目负责人用户不存在或已禁用";
            }
            if (!isAdmin && isDeptLeader) {
                if (currentUser.getDeptId() == null || !Objects.equals(currentUser.getDeptId(), leaderUser.getDeptId())) {
                    return "部门负责人仅可指定本部门人员为负责人";
                }
            }
            project.setLeaderName(StrUtil.isNotBlank(leaderUser.getRealName()) ? leaderUser.getRealName() : leaderUser.getUsername());
            project.setLeaderPhone(leaderUser.getPhone());
            return null;
        }

        if (!isAdmin && !isDeptLeader) {
            project.setLeaderName(StrUtil.isNotBlank(currentUser.getRealName()) ? currentUser.getRealName() : currentUser.getUsername());
            project.setLeaderPhone(currentUser.getPhone());
        }
        return null;
    }

    private boolean isDraftStatus(Integer status) {
        return status == null || Objects.equals(status, 0);
    }
}
