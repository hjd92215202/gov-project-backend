package com.gov.module.project.controller;

import com.gov.common.result.R;
import com.gov.module.flow.service.FlowService;
import com.gov.module.project.entity.BizProject;
import com.gov.module.project.service.BizProjectService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "工程项目管理")
@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private BizProjectService bizProjectService; // 需要配套建立 service 和 Mapper，和 SysUser 的套路一样

    @ApiOperation("录入工程信息")
    @PostMapping("/add")
    public R<String> add(@RequestBody BizProject project) {
        bizProjectService.save(project);
        return R.ok("工程录入成功");
    }

    @ApiOperation("获取工程详情(自动解密敏感字段)")
    @GetMapping("/get/{id}")
    public R<BizProject> get(@PathVariable Long id) {
        return R.ok(bizProjectService.getById(id));
    }


    @Autowired
    private FlowService flowService;
    @ApiOperation("录入并提交工程申请")
    @PostMapping("/submit")
    public R<String> submit(@RequestBody BizProject project) {
        // 1. 保存工程信息到数据库
        bizProjectService.save(project);

        // 2. 准备流程变量 (实际开发中，这些ID应从 sys_dept 表动态查出来)
        Map<String, Object> vars = new HashMap<>();
        vars.put("deptLeaderId", "1001"); // 假设这是部门负责人ID
        vars.put("topLeaderId", "1002");  // 假设这是上级领导ID

        // 3. 开启审批流，并将工程ID作为 BusinessKey 绑定
        flowService.startProcess(project.getId().toString(), vars);

        // 4. 更新工程状态为“审批中”
        project.setStatus(1);
        bizProjectService.updateById(project);

        return R.ok("工程已提交，进入审批流程");
    }
}