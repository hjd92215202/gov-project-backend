package com.gov.module.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gov.common.result.R;
import com.gov.module.flow.service.FlowService;
import com.gov.module.project.vo.FlowTaskVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(tags = "审批流管理")
@RestController
@RequestMapping("/flow")
public class FlowController {

    @Autowired
    private FlowService flowService;

    /**
     * 获取当前登录人的待办审批任务分页。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 待办任务分页结果
     */
    @ApiOperation("我的待办任务")
    @GetMapping("/todo")
    public R<IPage<FlowTaskVO>> getTodo(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return R.ok(flowService.getTodoPage(pageNum, pageSize));
    }

    /**
     * 获取当前登录人的已办审批任务分页。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 已办任务分页结果
     */
    @ApiOperation("我的已办任务")
    @GetMapping("/done")
    public R<IPage<FlowTaskVO>> getDone(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        return R.ok(flowService.getDonePage(pageNum, pageSize));
    }

    /**
     * 提交审批决定。
     * 该接口只负责承接前端的“同意 / 驳回”动作，真正的流程推进由 `FlowService` 完成。
     *
     * @param params 审批参数，包含 taskId 与 approved
     * @return 中文审批结果
     */
    @ApiOperation("审批任务")
    @PostMapping("/approve")
    public R<String> approve(@RequestBody Map<String, Object> params) {
        String taskId = params.get("taskId") == null ? null : String.valueOf(params.get("taskId"));
        Boolean approved = params.get("approved") instanceof Boolean ? (Boolean) params.get("approved") : null;

        if (taskId == null || approved == null) {
            return R.fail("审批参数不能为空");
        }

        flowService.approve(taskId, approved);
        return R.ok("审批操作成功");
    }
}
