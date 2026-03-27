package com.gov.module.flow.service;

import cn.dev33.satoken.stp.StpUtil;
import com.gov.module.project.entity.BizProject;
import com.gov.module.project.service.BizProjectService;
import com.gov.module.project.vo.FlowTaskVO;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
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

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

    /**
     * 启动审批流程。
     */
    public void startProcess(String businessKey, Map<String, Object> variables) {
        runtimeService.startProcessInstanceByKey("projectApproval", businessKey, variables);
    }

    /**
     * 查询当前用户待办任务。
     */
    public List<FlowTaskVO> getTodoList() {
        String userId = StpUtil.getLoginIdAsString();
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
     * 查询当前用户已办任务。
     */
    public List<FlowTaskVO> getDoneList() {
        String userId = StpUtil.getLoginIdAsString();
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();

        List<FlowTaskVO> voList = new ArrayList<>();
        for (HistoricTaskInstance task : tasks) {
            FlowTaskVO vo = new FlowTaskVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setCreateTime(task.getCreateTime());
            vo.setProcessInstanceId(task.getProcessInstanceId());

            HistoricProcessInstance process = historyService.createHistoricProcessInstanceQuery()
                    .processInstanceId(task.getProcessInstanceId())
                    .singleResult();
            if (process != null) {
                vo.setBusinessKey(process.getBusinessKey());
                BizProject project = bizProjectService.getById(process.getBusinessKey());
                if (project != null) {
                    vo.setProjectName(project.getProjectName());
                    vo.setLeaderName(project.getLeaderName());
                }
            }
            voList.add(vo);
        }
        return voList;
    }

    /**
     * 办理审批（同意/驳回），支持逐级上报。
     */
    @Transactional
    public void approve(String taskId, boolean approved) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("审批任务不存在");
        }
        String processInstanceId = task.getProcessInstanceId();
        String businessKey = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult().getBusinessKey();

        if (!approved) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("approved", false);
            taskService.complete(taskId, vars);
            updateProjectStatus(businessKey, 3);
            return;
        }

        if (task.getAssignee() == null) {
            throw new RuntimeException("审批任务缺少处理人");
        }
        Long currentUserId = Long.parseLong(task.getAssignee());
        SysUser currentUser = sysUserService.getById(currentUserId);
        if (currentUser == null) {
            throw new RuntimeException("审批处理人不存在");
        }
        if (currentUser.getDeptId() == null) {
            throw new RuntimeException("审批处理人未绑定部门");
        }
        SysDept currentDept = sysDeptService.getById(currentUser.getDeptId());
        if (currentDept == null) {
            throw new RuntimeException("审批处理人所属部门不存在");
        }

        SysDept parentDept = sysDeptService.getById(currentDept.getParentId());

        Map<String, Object> vars = new HashMap<>();
        vars.put("approved", true);

        if (parentDept != null && parentDept.getLeaderId() != null) {
            vars.put("hasNext", true);
            vars.put("currentAssignee", parentDept.getLeaderId().toString());
            updateProjectStatus(businessKey, 1);
        } else {
            vars.put("hasNext", false);
            updateProjectStatus(businessKey, 2);
        }

        taskService.complete(taskId, vars);
    }

    private void updateProjectStatus(String id, Integer status) {
        BizProject p = new BizProject();
        p.setId(Long.parseLong(id));
        p.setStatus(status);
        bizProjectService.updateById(p);
    }
}
