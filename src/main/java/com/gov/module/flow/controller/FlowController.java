package com.gov.module.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gov.common.result.R;
import com.gov.module.flow.dto.FlowApproveDTO;
import com.gov.module.flow.service.FlowService;
import com.gov.module.project.vo.FlowTaskVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@Api(tags = "审批流管理")
@RestController
@RequestMapping("/flow")
@Validated
public class FlowController {

    @Autowired
    private FlowService flowService;

    @ApiOperation("我的待办任务")
    @GetMapping("/todo")
    public R<IPage<FlowTaskVO>> getTodo(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return R.ok(flowService.getTodoPage(pageNum, pageSize));
    }

    @ApiOperation("我的已办任务")
    @GetMapping("/done")
    public R<IPage<FlowTaskVO>> getDone(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return R.ok(flowService.getDonePage(pageNum, pageSize));
    }

    @ApiOperation("审批任务")
    @PostMapping("/approve")
    public R<String> approve(@Valid @RequestBody FlowApproveDTO payload) {
        flowService.approve(payload.getTaskId(), payload.getApproved());
        return R.ok("审批操作成功");
    }
}
