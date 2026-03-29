package com.gov.module.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.gov.common.result.R;
import com.gov.module.flow.service.FlowService;
import com.gov.module.project.dto.ProjectCreateDTO;
import com.gov.module.project.dto.ProjectSubmitDTO;
import com.gov.module.project.dto.ProjectUpdateDTO;
import com.gov.module.project.entity.BizProject;
import com.gov.module.project.service.BizProjectService;
import com.gov.module.project.vo.ProjectDetailVO;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.UserAccessContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 职责：验证项目控制器的核心业务分支。
 * 为什么存在：项目状态机、数据权限和提交审批是项目模块最容易回归的部分。
 * 关键输入输出：输入为 DTO 和当前登录用户上下文，输出为业务响应与流程启动参数。
 * 关联链路：新增项目、编辑项目、查看项目、提交审批。
 */
@ExtendWith(MockitoExtension.class)
class ProjectControllerTest {

    private final ProjectController controller = new ProjectController();
    private final RecordingFlowService recordingFlowService = new RecordingFlowService();

    @Mock
    private BizProjectService bizProjectService;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysDeptService sysDeptService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "bizProjectService", bizProjectService);
        ReflectionTestUtils.setField(controller, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(controller, "sysDeptService", sysDeptService);
        ReflectionTestUtils.setField(controller, "flowService", recordingFlowService);
        recordingFlowService.businessKey = null;
        recordingFlowService.variables = null;
    }

    /**
     * 作用：验证普通用户新增项目时会自动保存为草稿，并回填本人为负责人。
     */
    @Test
    void add_shouldDefaultDraftAndUseCurrentUserAsLeaderForNormalUser() {
        ProjectCreateDTO payload = new ProjectCreateDTO();
        payload.setProjectName("示范项目");

        SysUser currentUser = new SysUser();
        currentUser.setId(100L);
        currentUser.setDeptId(9L);
        currentUser.setUsername("zhangsan");
        currentUser.setRealName("张三");
        currentUser.setPhone("13800000000");

        UserAccessContext context = new UserAccessContext();
        context.setUserId(100L);
        context.setDeptId(9L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(sysUserService.getById(100L)).thenReturn(currentUser);
            when(sysUserService.getAccessContext(100L)).thenReturn(context);

            R<String> result = controller.add(payload);

            ArgumentCaptor<BizProject> captor = ArgumentCaptor.forClass(BizProject.class);
            verify(bizProjectService).save(captor.capture());
            BizProject saved = captor.getValue();

            assertEquals(Integer.valueOf(200), result.getCode());
            assertEquals(Integer.valueOf(0), saved.getStatus());
            assertEquals(Long.valueOf(100L), saved.getCreatorId());
            assertEquals(Long.valueOf(9L), saved.getCreatorDeptId());
            assertEquals("张三", saved.getLeaderName());
            assertEquals("13800000000", saved.getLeaderPhone());
        }
    }

    /**
     * 作用：验证数据越权时项目详情接口不会返回实体内容。
     */
    @Test
    void get_shouldRejectProjectOutsideCurrentScope() {
        BizProject project = new BizProject();
        project.setId(1L);
        project.setCreatorId(200L);
        project.setCreatorDeptId(20L);

        UserAccessContext context = new UserAccessContext();
        context.setUserId(100L);
        context.setDeptId(9L);

        when(bizProjectService.getById(1L)).thenReturn(project);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(sysUserService.getAccessContext(100L)).thenReturn(context);

            R<ProjectDetailVO> result = controller.get(1L);

            assertEquals(Integer.valueOf(403), result.getCode());
            assertNull(result.getData());
        }
    }

    /**
     * 作用：验证审批中的项目不能再次编辑，保护状态机规则。
     */
    @Test
    void update_shouldRejectProjectInApprovalStatus() {
        ProjectUpdateDTO payload = new ProjectUpdateDTO();
        payload.setId(1L);
        payload.setProjectName("更新项目");

        BizProject dbProject = new BizProject();
        dbProject.setId(1L);
        dbProject.setStatus(1);

        when(bizProjectService.getById(1L)).thenReturn(dbProject);

        R<String> result = controller.update(payload);

        assertEquals(Integer.valueOf(403), result.getCode());
        assertEquals("仅草稿和驳回状态项目可编辑", result.getMsg());
    }

    /**
     * 作用：验证管理员可删除已通过状态项目，满足运维纠偏场景。
     */
    @Test
    void delete_shouldAllowApprovedProjectForAdmin() {
        BizProject dbProject = new BizProject();
        dbProject.setId(1L);
        dbProject.setStatus(2);

        UserAccessContext context = new UserAccessContext();
        context.setUserId(1L);
        context.setAdmin(true);

        when(bizProjectService.getById(1L)).thenReturn(dbProject);
        when(sysUserService.getAccessContext(1L)).thenReturn(context);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);

            R<String> result = controller.delete(1L);

            assertEquals(Integer.valueOf(200), result.getCode());
            verify(bizProjectService).removeById(1L);
        }
    }

    /**
     * 作用：验证联系电话格式非法时会在接口层被拦截。
     */
    @Test
    void add_shouldRejectInvalidLeaderPhone() {
        ProjectCreateDTO payload = new ProjectCreateDTO();
        payload.setProjectName("示范项目");
        payload.setLeaderPhone("12345");

        SysUser currentUser = new SysUser();
        currentUser.setId(100L);
        currentUser.setDeptId(9L);
        currentUser.setUsername("zhangsan");

        UserAccessContext context = new UserAccessContext();
        context.setUserId(100L);
        context.setDeptId(9L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(sysUserService.getById(100L)).thenReturn(currentUser);
            when(sysUserService.getAccessContext(100L)).thenReturn(context);

            R<String> result = controller.add(payload);

            assertEquals(Integer.valueOf(500), result.getCode());
            assertEquals("联系电话格式不正确，请填写7到20位数字，可包含短横线", result.getMsg());
            verify(bizProjectService, never()).save(any(BizProject.class));
        }
    }

    /**
     * 作用：验证手工填写联系电话时不会被负责人档案电话覆盖。
     */
    @Test
    void add_shouldKeepManualLeaderPhoneWhenLeaderSelected() {
        ProjectCreateDTO payload = new ProjectCreateDTO();
        payload.setProjectName("示范项目");
        payload.setLeaderUserId(300L);
        payload.setLeaderPhone("13900000000");

        SysUser currentUser = new SysUser();
        currentUser.setId(1L);
        currentUser.setDeptId(9L);
        currentUser.setUsername("admin");

        SysUser leaderUser = new SysUser();
        leaderUser.setId(300L);
        leaderUser.setStatus(1);
        leaderUser.setUsername("lisi");
        leaderUser.setRealName("李四");
        leaderUser.setPhone("13800000000");

        UserAccessContext context = new UserAccessContext();
        context.setUserId(1L);
        context.setDeptId(9L);
        context.setAdmin(true);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(1L);
            when(sysUserService.getById(1L)).thenReturn(currentUser);
            when(sysUserService.getById(300L)).thenReturn(leaderUser);
            when(sysUserService.getAccessContext(1L)).thenReturn(context);

            R<String> result = controller.add(payload);

            ArgumentCaptor<BizProject> captor = ArgumentCaptor.forClass(BizProject.class);
            verify(bizProjectService).save(captor.capture());
            assertEquals(Integer.valueOf(200), result.getCode());
            assertEquals("13900000000", captor.getValue().getLeaderPhone());
        }
    }

    /**
     * 作用：验证可编辑项目提交审批时会正确更新状态并启动流程。
     */
    @Test
    void submit_shouldStartFlowForEditableExistingProject() {
        ProjectSubmitDTO payload = new ProjectSubmitDTO();
        payload.setId(1L);

        SysUser currentUser = new SysUser();
        currentUser.setId(100L);
        currentUser.setDeptId(9L);

        SysUser deptLeader = new SysUser();
        deptLeader.setId(200L);

        BizProject dbProject = new BizProject();
        dbProject.setId(1L);
        dbProject.setStatus(0);
        dbProject.setCreatorId(100L);
        dbProject.setCreatorDeptId(9L);

        SysDept dept = new SysDept();
        dept.setId(9L);
        dept.setLeaderId(200L);

        UserAccessContext context = new UserAccessContext();
        context.setUserId(100L);
        context.setDeptId(9L);

        try (MockedStatic<StpUtil> stp = mockStatic(StpUtil.class)) {
            stp.when(StpUtil::getLoginIdAsLong).thenReturn(100L);
            when(sysUserService.getById(100L)).thenReturn(currentUser);
            when(sysUserService.getById(200L)).thenReturn(deptLeader);
            when(sysUserService.getAccessContext(100L)).thenReturn(context);
            when(bizProjectService.getById(1L)).thenReturn(dbProject);
            when(sysDeptService.getById(9L)).thenReturn(dept);

            R<String> result = controller.submit(payload);

            ArgumentCaptor<BizProject> projectCaptor = ArgumentCaptor.forClass(BizProject.class);
            verify(bizProjectService).updateById(projectCaptor.capture());
            assertEquals(Long.valueOf(1L), projectCaptor.getValue().getId());
            assertEquals(Integer.valueOf(1), projectCaptor.getValue().getStatus());

            assertEquals("1", recordingFlowService.businessKey);
            assertEquals("200", recordingFlowService.variables.get("currentAssignee"));
            assertEquals(Integer.valueOf(200), result.getCode());
        }
    }

    /**
     * 职责：记录项目控制器发给流程服务的启动参数。
     * 为什么存在：单元测试只需断言控制器有没有正确交接给流程层。
     */
    static class RecordingFlowService extends FlowService {
        private String businessKey;
        private Map<String, Object> variables;

        @Override
        public void startProcess(String businessKey, Map<String, Object> variables) {
            this.businessKey = businessKey;
            this.variables = variables;
        }
    }
}
