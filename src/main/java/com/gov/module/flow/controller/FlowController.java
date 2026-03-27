package com.gov.module.flow.controller;

import com.gov.common.result.R;
import com.gov.module.flow.service.FlowService;
import com.gov.module.project.vo.FlowTaskVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Api(tags = "审批流管理")
@RestController
@RequestMapping("/flow")
public class FlowController {

    @Autowired
    private FlowService flowService;

    @ApiOperation("获取我的待办任务")
    @GetMapping("/todo")
    public R<List<FlowTaskVO>> getTodo() {
        return R.ok(flowService.getTodoList());
    }

    @ApiOperation("获取我的已办任务")
    @GetMapping("/done")
    public R<List<FlowTaskVO>> getDone() {
        return R.ok(flowService.getDoneList());
    }

    @ApiOperation("审批任务(同意或驳回)")
    @PostMapping("/approve")
    public R<String> approve(@RequestBody Map<String, Object> params) {
        String taskId = (String) params.get("taskId");
        Boolean approved = (Boolean) params.get("approved");

        if (taskId == null || approved == null) {
            return R.fail("参数不能为空");
        }

        flowService.approve(taskId, approved);
        return R.ok("操作成功");
    }
}
