package com.gov.module.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "工程项目管理")
@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private BizProjectService bizProjectService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

    @ApiOperation("录入工程信息")
    @PostMapping("/add")
    public R<String> add(@RequestBody BizProject project) {
        if (StrUtil.isBlank(project.getProjectName())) {
            return R.fail("项目名称不能为空");
        }
        if (project.getStatus() == null) {
            project.setStatus(0);
        }
        bizProjectService.save(project);
        return R.ok("工程录入成功");
    }

    @ApiOperation("分页查询工程信息")
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
        queryWrapper.orderByDesc(BizProject::getCreateTime);

        Page<BizProject> page = new Page<>(pageNum, pageSize);
        IPage<BizProject> result = bizProjectService.page(page, queryWrapper);
        return R.ok(result);
    }

    @ApiOperation("获取工程详情(自动解密敏感字段)")
    @GetMapping("/get/{id}")
    public R<BizProject> get(@PathVariable Long id) {
        return R.ok(bizProjectService.getById(id));
    }

    @ApiOperation("更新工程信息")
    @PutMapping("/update")
    public R<String> update(@RequestBody BizProject project) {
        if (project.getId() == null) {
            return R.fail("项目ID不能为空");
        }
        if (StrUtil.isBlank(project.getProjectName())) {
            return R.fail("项目名称不能为空");
        }
        bizProjectService.updateById(project);
        return R.ok("工程更新成功");
    }

    @Autowired
    private FlowService flowService;
    @ApiOperation("录入并提交工程申请")
    @PostMapping("/submit")
    public R<String> submit(@RequestBody BizProject project) {
        if (StrUtil.isBlank(project.getProjectName())) {
            return R.fail("项目名称不能为空");
        }
        Long projectId;
        if (project.getId() != null && bizProjectService.getById(project.getId()) != null) {
            project.setStatus(1);
            bizProjectService.updateById(project);
            projectId = project.getId();
        } else {
            project.setStatus(1);
            bizProjectService.save(project);
            projectId = project.getId();
        }

        // 1. 获取发起人所属部门负责人
        SysUser loginUser = sysUserService.getById(StpUtil.getLoginIdAsLong());
        SysDept dept = sysDeptService.getById(loginUser.getDeptId());
        if (dept == null || dept.getLeaderId() == null) {
            return R.fail("当前用户所属部门未配置负责人，无法提交审批");
        }

        // 2. 初始变量
        Map<String, Object> vars = new HashMap<>();
        vars.put("currentAssignee", dept.getLeaderId().toString());

        // 3. 启动流程
        flowService.startProcess(projectId.toString(), vars);

        return R.ok("提交成功，请等待部门负责人审核");
    }

    @ApiOperation("获取地图展示数据(支持按省市县筛选)")
    @GetMapping("/map/list")
    public R<List<ProjectMapVO>> getMapList(
            @RequestParam(required = false) String province,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String district) {

        // 1. 构建查询条件
        LambdaQueryWrapper<BizProject> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.select(BizProject::getId, BizProject::getProjectName,
                BizProject::getAddress, BizProject::getLongitude,
                BizProject::getLatitude, BizProject::getProvince,
                BizProject::getCity, BizProject::getDistrict);

        // 2. 动态筛选（实现下钻逻辑的核心）
        if (StrUtil.isNotBlank(province)) queryWrapper.eq(BizProject::getProvince, province);
        if (StrUtil.isNotBlank(city)) queryWrapper.eq(BizProject::getCity, city);
        if (StrUtil.isNotBlank(district)) queryWrapper.eq(BizProject::getDistrict, district);

        // 3. 转换并返回
        List<BizProject> list = bizProjectService.list(queryWrapper);
        // 这里可以使用 BeanUtil 快速转换，减少代码量
        List<ProjectMapVO> voList = JSONUtil.toList(JSONUtil.parseArray(list), ProjectMapVO.class);

        return R.ok(voList);
    }
}
