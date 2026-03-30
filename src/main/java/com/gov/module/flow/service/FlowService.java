package com.gov.module.flow.service;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.module.project.entity.BizProject;
import com.gov.module.project.service.BizProjectService;
import com.gov.module.project.vo.FlowTaskVO;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.LongSupplier;
import java.util.stream.Collectors;

/**
 * 审批流业务服务。
 * 这个类存在的意义是把 Flowable 引擎调用、项目状态流转、部门层级审批推进统一收口，
 * 避免 controller 直接操纵流程引擎和业务状态。
 */
@Service
public class FlowService {

    private static final Logger perfLog = LoggerFactory.getLogger("com.gov.perf");

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
     * 启动项目审批流程实例。
     *
     * @param businessKey 业务主键，这里对应项目 ID
     * @param variables 启动变量，至少包含首个审批人
     */
    public void startProcess(String businessKey, Map<String, Object> variables) {
        runtimeService.startProcessInstanceByKey("projectApproval", businessKey, variables);
    }

    /**
     * 查询当前登录人的待办任务分页。
     * 这里会把 Flowable 任务、流程实例和项目基础信息组装成前端可直接展示的 `FlowTaskVO`。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 待办任务分页
     */
    public IPage<FlowTaskVO> getTodoPage(Integer pageNum, Integer pageSize) {
        long startAt = System.currentTimeMillis();
        String userId = StpUtil.getLoginIdAsString();
        int current = normalizePageNum(pageNum);
        int size = normalizePageSize(pageSize);
        int offset = (current - 1) * size;

        long listStartAt = System.currentTimeMillis();
        List<Task> tasks = taskService.createTaskQuery()
                .taskAssignee(userId)
                .orderByTaskCreateTime().desc()
                .listPage(offset, size);
        long listMs = System.currentTimeMillis() - listStartAt;

        long countStartAt = System.currentTimeMillis();
        long total = resolvePagedTotal(current, size, tasks.size(),
                () -> taskService.createTaskQuery().taskAssignee(userId).count());
        long countMs = System.currentTimeMillis() - countStartAt;

        long processStartAt = System.currentTimeMillis();
        Map<String, String> processBusinessKeyMap = loadRuntimeBusinessKeys(
                tasks.stream().map(Task::getProcessInstanceId).collect(Collectors.toCollection(LinkedHashSet::new))
        );
        long processMs = System.currentTimeMillis() - processStartAt;

        long projectStartAt = System.currentTimeMillis();
        Map<String, BizProject> projectMap = loadProjectMap(processBusinessKeyMap.values());
        long projectMs = System.currentTimeMillis() - projectStartAt;

        long assembleStartAt = System.currentTimeMillis();
        List<FlowTaskVO> records = new ArrayList<>();
        for (Task task : tasks) {
            FlowTaskVO vo = new FlowTaskVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setCreateTime(task.getCreateTime());
            vo.setProcessInstanceId(task.getProcessInstanceId());

            String businessKey = processBusinessKeyMap.get(task.getProcessInstanceId());
            vo.setBusinessKey(businessKey);
            BizProject project = projectMap.get(businessKey);
            if (project != null) {
                vo.setProjectName(project.getProjectName());
                vo.setLeaderName(project.getLeaderName());
            }
            records.add(vo);
        }
        long assembleMs = System.currentTimeMillis() - assembleStartAt;

        IPage<FlowTaskVO> page = buildPage(current, size, total, records);
        long totalDurationMs = System.currentTimeMillis() - startAt;
        perfLog.info(
                "审批待办查询完成 userId={} pageNum={} pageSize={} total={} records={} listMs={} countMs={} processMs={} projectMs={} assembleMs={} durationMs={}",
                userId, current, size, total, records.size(), listMs, countMs, processMs, projectMs, assembleMs, totalDurationMs
        );
        if (totalDurationMs >= 2000) {
            perfLog.warn(
                    "审批待办查询慢调用 userId={} pageNum={} pageSize={} total={} records={} listMs={} countMs={} processMs={} projectMs={} assembleMs={} durationMs={}",
                    userId, current, size, total, records.size(), listMs, countMs, processMs, projectMs, assembleMs, totalDurationMs
            );
        }
        return page;
    }

    /**
     * 查询当前登录人的已办任务分页。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 已办任务分页
     */
    public IPage<FlowTaskVO> getDonePage(Integer pageNum, Integer pageSize) {
        long startAt = System.currentTimeMillis();
        String userId = StpUtil.getLoginIdAsString();
        int current = normalizePageNum(pageNum);
        int size = normalizePageSize(pageSize);
        int offset = (current - 1) * size;

        long listStartAt = System.currentTimeMillis();
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .taskAssignee(userId)
                .finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .listPage(offset, size);
        long listMs = System.currentTimeMillis() - listStartAt;

        long countStartAt = System.currentTimeMillis();
        long total = resolvePagedTotal(current, size, tasks.size(),
                () -> historyService.createHistoricTaskInstanceQuery()
                        .taskAssignee(userId)
                        .finished()
                        .count());
        long countMs = System.currentTimeMillis() - countStartAt;

        long processStartAt = System.currentTimeMillis();
        Map<String, HistoricProcessInstance> processMap = loadHistoricProcesses(
                tasks.stream().map(HistoricTaskInstance::getProcessInstanceId).collect(Collectors.toCollection(LinkedHashSet::new))
        );
        long processMs = System.currentTimeMillis() - processStartAt;

        long projectStartAt = System.currentTimeMillis();
        Map<String, BizProject> projectMap = loadProjectMap(processMap.values().stream()
                .map(HistoricProcessInstance::getBusinessKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        long projectMs = System.currentTimeMillis() - projectStartAt;

        long assembleStartAt = System.currentTimeMillis();
        List<FlowTaskVO> records = new ArrayList<>();
        for (HistoricTaskInstance task : tasks) {
            FlowTaskVO vo = new FlowTaskVO();
            vo.setTaskId(task.getId());
            vo.setTaskName(task.getName());
            vo.setCreateTime(task.getCreateTime());
            vo.setProcessInstanceId(task.getProcessInstanceId());

            HistoricProcessInstance process = processMap.get(task.getProcessInstanceId());
            if (process != null) {
                vo.setBusinessKey(process.getBusinessKey());
                BizProject project = projectMap.get(process.getBusinessKey());
                if (project != null) {
                    vo.setProjectName(project.getProjectName());
                    vo.setLeaderName(project.getLeaderName());
                }
            }
            records.add(vo);
        }
        long assembleMs = System.currentTimeMillis() - assembleStartAt;

        IPage<FlowTaskVO> page = buildPage(current, size, total, records);
        long totalDurationMs = System.currentTimeMillis() - startAt;
        perfLog.info(
                "审批已办查询完成 userId={} pageNum={} pageSize={} total={} records={} listMs={} countMs={} processMs={} projectMs={} assembleMs={} durationMs={}",
                userId, current, size, total, records.size(), listMs, countMs, processMs, projectMs, assembleMs, totalDurationMs
        );
        if (totalDurationMs >= 2000) {
            perfLog.warn(
                    "审批已办查询慢调用 userId={} pageNum={} pageSize={} total={} records={} listMs={} countMs={} processMs={} projectMs={} assembleMs={} durationMs={}",
                    userId, current, size, total, records.size(), listMs, countMs, processMs, projectMs, assembleMs, totalDurationMs
            );
        }
        return page;
    }

    /**
     * 执行审批动作。
     * 这里同时负责推进 Flowable 流程与维护项目业务状态，
     * 是“流程引擎状态”和“业务表状态”同步的关键节点。
     *
     * @param taskId 任务 ID
     * @param approved 是否同意
     */
    @Transactional
    public void approve(String taskId, boolean approved) {
        long startAt = System.currentTimeMillis();
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if (task == null) {
            throw new RuntimeException("审批任务不存在");
        }
        String currentLoginId = StpUtil.getLoginIdAsString();
        if (task.getAssignee() == null || !Objects.equals(task.getAssignee(), currentLoginId)) {
            throw new RuntimeException("当前任务不属于当前登录用户");
        }

        String processInstanceId = task.getProcessInstanceId();
        String businessKey = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult()
                .getBusinessKey();

        if (!approved) {
            Map<String, Object> vars = new HashMap<>();
            vars.put("approved", false);
            taskService.complete(taskId, vars);
            updateProjectStatus(businessKey, 3);
            perfLog.info("审批任务处理完成 userId={} taskId={} approved=false businessKey={} durationMs={}",
                    currentLoginId, taskId, businessKey, System.currentTimeMillis() - startAt);
            return;
        }

        if (task.getAssignee() == null) {
            throw new RuntimeException("审批任务未分配处理人");
        }
        Long currentUserId = Long.parseLong(task.getAssignee());
        SysUser currentUser = sysUserService.getById(currentUserId);
        if (currentUser == null) {
            throw new RuntimeException("审批人不存在");
        }
        if (currentUser.getDeptId() == null) {
            throw new RuntimeException("审批人未绑定部门");
        }
        SysDept currentDept = sysDeptService.getById(currentUser.getDeptId());
        if (currentDept == null) {
            throw new RuntimeException("审批人所属部门不存在");
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
        perfLog.info("审批任务处理完成 userId={} taskId={} approved=true businessKey={} durationMs={}",
                currentLoginId, taskId, businessKey, System.currentTimeMillis() - startAt);
    }

    /**
     * 批量读取运行中流程实例与业务主键映射。
     *
     * @param processInstanceIds 流程实例 ID 集合
     * @return 流程实例 ID 到业务主键的映射
     */
    private Map<String, String> loadRuntimeBusinessKeys(Set<String> processInstanceIds) {
        if (processInstanceIds == null || processInstanceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return runtimeService.createProcessInstanceQuery()
                .processInstanceIds(processInstanceIds)
                .list()
                .stream()
                .filter(item -> item.getProcessInstanceId() != null && item.getBusinessKey() != null)
                .collect(Collectors.toMap(
                        item -> item.getProcessInstanceId(),
                        item -> item.getBusinessKey(),
                        (a, b) -> a
                ));
    }

    /**
     * 批量读取历史流程实例。
     *
     * @param processInstanceIds 流程实例 ID 集合
     * @return 流程实例 ID 到历史流程实例对象的映射
     */
    private Map<String, HistoricProcessInstance> loadHistoricProcesses(Set<String> processInstanceIds) {
        if (processInstanceIds == null || processInstanceIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return historyService.createHistoricProcessInstanceQuery()
                .processInstanceIds(processInstanceIds)
                .list()
                .stream()
                .filter(item -> item.getId() != null)
                .collect(Collectors.toMap(
                        HistoricProcessInstance::getId,
                        item -> item,
                        (a, b) -> a
                ));
    }

    /**
     * 按业务主键批量加载项目最小信息。
     * 这里只取审批列表展示所需字段，避免不必要的全字段查询。
     *
     * @param businessKeys 业务主键集合
     * @return 项目映射
     */
    private Map<String, BizProject> loadProjectMap(Collection<String> businessKeys) {
        if (businessKeys == null || businessKeys.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> projectIds = businessKeys.stream()
                .filter(Objects::nonNull)
                .map(key -> {
                    try {
                        return Long.parseLong(key);
                    } catch (NumberFormatException error) {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (projectIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return bizProjectService.list(new LambdaQueryWrapper<BizProject>()
                        .select(BizProject::getId, BizProject::getProjectName, BizProject::getLeaderName)
                        .in(BizProject::getId, projectIds))
                .stream()
                .collect(Collectors.toMap(
                        item -> String.valueOf(item.getId()),
                        item -> item,
                        (a, b) -> a
                ));
    }

    private IPage<FlowTaskVO> buildPage(int current, int size, long total, List<FlowTaskVO> records) {
        Page<FlowTaskVO> page = new Page<>(current, size, total);
        page.setRecords(records);
        return page;
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    private long resolvePagedTotal(int current, int size, int currentPageRecords, LongSupplier totalCountSupplier) {
        if (current <= 1 && currentPageRecords < size) {
            return currentPageRecords;
        }
        return totalCountSupplier.getAsLong();
    }

    private void updateProjectStatus(String id, Integer status) {
        BizProject project = new BizProject();
        project.setId(Long.parseLong(id));
        project.setStatus(status);
        bizProjectService.updateById(project);
    }
}
