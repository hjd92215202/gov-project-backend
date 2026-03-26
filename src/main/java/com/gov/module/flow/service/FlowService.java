package com.gov.module.flow.service;

import cn.dev33.satoken.stp.StpUtil;
import com.gov.module.project.entity.BizProject;
import com.gov.module.project.service.BizProjectService;
import com.gov.module.project.vo.FlowTaskVO;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FlowService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private BizProjectService bizProjectService;

    /**
     * 1. 启动流程
     */
    public void startProcess(String businessKey, Map<String, Object> variables) {
        runtimeService.startProcessInstanceByKey("projectApproval", businessKey, variables);
    }

    /**
     * 2. 查询当前用户的待办任务
     */
    public List<FlowTaskVO> getTodoList() {
        String userId = StpUtil.getLoginIdAsString();
        // 查询指派给我的任务
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime().desc()
                .list();

        List<FlowTaskVO> voList = new ArrayList<>();
        for (Task task : tasks) {
            FlowTaskVO vo = new FlowTaskVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setCreateTime(task.getCreateTime());
            vo.setProcessInstanceId(task.getProcessInstanceId());

            // 通过 BusinessKey 拿到关联的项目信息
            String businessKey = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult().getBusinessKey();

            vo.setBusinessKey(businessKey);
            BizProject project = bizProjectService.getById(businessKey);
            if (project != null) {
                vo.setProjectName(project.getProjectName());
                vo.setLeaderName(project.getLeaderName());
            }
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 3. 办理审批 (同意/驳回)
     */
    @Transactional
    public void approve(String taskId, boolean approved) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        String businessKey = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult().getBusinessKey();

        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);

        // 1. 完成当前任务
        taskService.complete(taskId, variables);

        // 2. 更新业务状态
        BizProject project = new BizProject();
        project.setId(Long.parseLong(businessKey));

        if (!approved) {
            project.setStatus(3); // 状态：被驳回/退回
        } else {
            // 检查流程是否真的结束了
            // 注意：使用 historyService 检查流程实例是否已结束更准确
            long activeCount = runtimeService.createProcessInstanceQuery()
                    .processInstanceId(processInstanceId).count();
            if (activeCount == 0) {
                project.setStatus(2); // 状态：已通过
            } else {
                project.setStatus(1); // 状态：下一级审批中
            }
        }
        bizProjectService.updateById(project);
    }
}