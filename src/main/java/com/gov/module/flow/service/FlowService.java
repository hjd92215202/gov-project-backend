package com.gov.module.flow.service;

import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
public class FlowService {

    @Autowired
    private RuntimeService runtimeService; // 运行时服务，用于启动流程

    @Autowired
    private TaskService taskService;       // 任务服务，用于审批任务

    /**
     * 1. 开启流程
     * @param businessKey 业务ID (就是我们的工程项目ID)
     * @param variables 流程变量 (包含审批人ID等)
     */
    public void startProcess(String businessKey, Map<String, Object> variables) {
        runtimeService.startProcessInstanceByKey("projectApproval", businessKey, variables);
    }

    /**
     * 2. 办理审批
     * @param taskId 任务ID
     * @param approved 是否通过
     */
    @Transactional
    public void completeTask(String taskId, boolean approved) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("approved", approved);
        taskService.complete(taskId, variables);

        // 逻辑：如果 approved 为 false，由于我们在 XML 里画了线连回 startNode，
        // 流程会自动流转回去。
    }
}