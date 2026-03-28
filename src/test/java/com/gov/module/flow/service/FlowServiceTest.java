package com.gov.module.flow.service;

import com.gov.module.project.entity.BizProject;
import com.gov.module.project.service.BizProjectService;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 职责：验证审批服务的关键状态流转逻辑。
 * 为什么存在：审批服务直接影响项目状态机和逐级审批流，属于最值得保护的业务核心。
 * 关键输入输出：输入为任务 ID 与审批结果，输出为流程变量变化和项目状态更新。
 * 关联链路：审批通过、审批驳回、向上级部门继续流转。
 */
@ExtendWith(MockitoExtension.class)
class FlowServiceTest {

    @InjectMocks
    private FlowService flowService;

    @Mock
    private RuntimeService runtimeService;

    @Mock
    private TaskService taskService;

    @Mock
    private HistoryService historyService;

    @Mock
    private BizProjectService bizProjectService;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysDeptService sysDeptService;

    /**
     * 作用：验证审批驳回时项目会被标记为“已驳回”。
     */
    @Test
    void approve_shouldRejectTaskAndMarkProjectRejected() {
        TaskQuery taskQuery = mock(TaskQuery.class);
        ProcessInstanceQuery processQuery = mock(ProcessInstanceQuery.class);
        Task task = mock(Task.class);
        ProcessInstance processInstance = mock(ProcessInstance.class);

        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId("task-1")).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);
        when(task.getProcessInstanceId()).thenReturn("proc-1");

        when(runtimeService.createProcessInstanceQuery()).thenReturn(processQuery);
        when(processQuery.processInstanceId("proc-1")).thenReturn(processQuery);
        when(processQuery.singleResult()).thenReturn(processInstance);
        when(processInstance.getBusinessKey()).thenReturn("88");

        flowService.approve("task-1", false);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(taskService).complete(eq("task-1"), varsCaptor.capture());
        assertEquals(Boolean.FALSE, varsCaptor.getValue().get("approved"));

        ArgumentCaptor<BizProject> projectCaptor = ArgumentCaptor.forClass(BizProject.class);
        verify(bizProjectService).updateById(projectCaptor.capture());
        assertEquals(Long.valueOf(88L), projectCaptor.getValue().getId());
        assertEquals(Integer.valueOf(3), projectCaptor.getValue().getStatus());
    }

    /**
     * 作用：验证审批通过且存在上级部门负责人时，会继续向上流转而不是直接结束流程。
     */
    @Test
    void approve_shouldForwardToParentLeaderWhenNextDepartmentExists() {
        TaskQuery taskQuery = mock(TaskQuery.class);
        ProcessInstanceQuery processQuery = mock(ProcessInstanceQuery.class);
        Task task = mock(Task.class);
        ProcessInstance processInstance = mock(ProcessInstance.class);

        SysUser approver = new SysUser();
        approver.setId(200L);
        approver.setDeptId(10L);

        SysDept currentDept = new SysDept();
        currentDept.setId(10L);
        currentDept.setParentId(11L);

        SysDept parentDept = new SysDept();
        parentDept.setId(11L);
        parentDept.setLeaderId(300L);

        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId("task-2")).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);
        when(task.getProcessInstanceId()).thenReturn("proc-2");
        when(task.getAssignee()).thenReturn("200");

        when(runtimeService.createProcessInstanceQuery()).thenReturn(processQuery);
        when(processQuery.processInstanceId("proc-2")).thenReturn(processQuery);
        when(processQuery.singleResult()).thenReturn(processInstance);
        when(processInstance.getBusinessKey()).thenReturn("99");

        when(sysUserService.getById(200L)).thenReturn(approver);
        when(sysDeptService.getById(10L)).thenReturn(currentDept);
        when(sysDeptService.getById(11L)).thenReturn(parentDept);

        flowService.approve("task-2", true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(taskService).complete(eq("task-2"), varsCaptor.capture());
        assertEquals(Boolean.TRUE, varsCaptor.getValue().get("approved"));
        assertEquals(Boolean.TRUE, varsCaptor.getValue().get("hasNext"));
        assertEquals("300", varsCaptor.getValue().get("currentAssignee"));

        ArgumentCaptor<BizProject> projectCaptor = ArgumentCaptor.forClass(BizProject.class);
        verify(bizProjectService).updateById(projectCaptor.capture());
        assertEquals(Integer.valueOf(1), projectCaptor.getValue().getStatus());
    }

    /**
     * 作用：验证审批通过且不存在上级负责人时，流程会结束并把项目标记为“已通过”。
     */
    @Test
    void approve_shouldFinishFlowWhenNoParentLeaderExists() {
        TaskQuery taskQuery = mock(TaskQuery.class);
        ProcessInstanceQuery processQuery = mock(ProcessInstanceQuery.class);
        Task task = mock(Task.class);
        ProcessInstance processInstance = mock(ProcessInstance.class);

        SysUser approver = new SysUser();
        approver.setId(210L);
        approver.setDeptId(20L);

        SysDept currentDept = new SysDept();
        currentDept.setId(20L);
        currentDept.setParentId(0L);

        when(taskService.createTaskQuery()).thenReturn(taskQuery);
        when(taskQuery.taskId("task-3")).thenReturn(taskQuery);
        when(taskQuery.singleResult()).thenReturn(task);
        when(task.getProcessInstanceId()).thenReturn("proc-3");
        when(task.getAssignee()).thenReturn("210");

        when(runtimeService.createProcessInstanceQuery()).thenReturn(processQuery);
        when(processQuery.processInstanceId("proc-3")).thenReturn(processQuery);
        when(processQuery.singleResult()).thenReturn(processInstance);
        when(processInstance.getBusinessKey()).thenReturn("100");

        when(sysUserService.getById(210L)).thenReturn(approver);
        when(sysDeptService.getById(20L)).thenReturn(currentDept);
        when(sysDeptService.getById(0L)).thenReturn(null);

        flowService.approve("task-3", true);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> varsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(taskService).complete(eq("task-3"), varsCaptor.capture());
        assertEquals(Boolean.FALSE, varsCaptor.getValue().get("hasNext"));
        assertFalse(varsCaptor.getValue().containsKey("currentAssignee"));

        ArgumentCaptor<BizProject> projectCaptor = ArgumentCaptor.forClass(BizProject.class);
        verify(bizProjectService).updateById(projectCaptor.capture());
        assertEquals(Integer.valueOf(2), projectCaptor.getValue().getStatus());
    }
}
