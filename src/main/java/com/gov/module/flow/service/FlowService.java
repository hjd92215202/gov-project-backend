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

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

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
    /**
     * 办理审批 (动态多级版)
     */
    @Transactional
    public void approve(String taskId, boolean approved) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String processInstanceId = task.getProcessInstanceId();
        String businessKey = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult().getBusinessKey();

        if (!approved) {
            // 如果驳回，设置变量让网关走 rejectEnd
            Map<String, Object> vars = new HashMap<>();
            vars.put("approved", false);
            taskService.complete(taskId, vars);

            // 更新业务表
            updateProjectStatus(businessKey, 3); // 3-已驳回
            return;
        }

        // --- 如果是“同意”，需要寻找下一个审批人 ---
        // 1. 获取当前审批人的部门信息
        Long currentUserId = Long.parseLong(task.getAssignee());
        SysUser currentUser = sysUserService.getById(currentUserId);
        SysDept currentDept = sysDeptService.getById(currentUser.getDeptId());

        // 2. 寻找上级部门及负责人
        SysDept parentDept = sysDeptService.getById(currentDept.getParentId());

        Map<String, Object> vars = new HashMap<>();
        vars.put("approved", true);

        if (parentDept != null && parentDept.getLeaderId() != null) {
            // 还有上级，设置变量继续循环
            vars.put("hasNext", true);
            vars.put("currentAssignee", parentDept.getLeaderId().toString());
            updateProjectStatus(businessKey, 1); // 依然在审批中
        } else {
            // 到头了，没有上级了
            vars.put("hasNext", false);
            updateProjectStatus(businessKey, 2); // 终审通过！
        }

        // 3. 提交任务
        taskService.complete(taskId, vars);
    }

    private void updateProjectStatus(String id, Integer status) {
        BizProject p = new BizProject();
        p.setId(Long.parseLong(id));
        p.setStatus(status);
        bizProjectService.updateById(p);
    }
}