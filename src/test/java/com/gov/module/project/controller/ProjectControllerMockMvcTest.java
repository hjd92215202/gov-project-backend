package com.gov.module.project.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.module.flow.service.FlowService;
import com.gov.module.project.dto.ProjectCreateDTO;
import com.gov.module.project.entity.BizProject;
import com.gov.module.project.service.BizProjectService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 职责：验证项目控制器在 Web 层的接口契约。
 * 为什么存在：项目新增和详情查看涉及权限与默认值补全，前端很依赖这些返回语义。
 * 关键输入输出：输入为项目新增 JSON 和项目详情请求，输出为统一 `R` 响应。
 * 关联链路：项目管理列表、新增项目、详情查看。
 */
@ExtendWith(MockitoExtension.class)
class ProjectControllerMockMvcTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RecordingFlowService flowService = new RecordingFlowService();
    private MockMvc mockMvc;

    @Mock
    private BizProjectService bizProjectService;

    @Mock
    private SysUserService sysUserService;

    @Mock
    private SysDeptService sysDeptService;

    @BeforeEach
    void setUp() {
        ProjectController controller = new ProjectController();
        ReflectionTestUtils.setField(controller, "bizProjectService", bizProjectService);
        ReflectionTestUtils.setField(controller, "sysUserService", sysUserService);
        ReflectionTestUtils.setField(controller, "sysDeptService", sysDeptService);
        ReflectionTestUtils.setField(controller, "flowService", flowService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        flowService.businessKey = null;
        flowService.variables = null;
    }

    /**
     * 作用：验证新增项目接口会返回成功响应，并把普通用户默认负责人补全到项目实体。
     */
    @Test
    void add_shouldReturnSuccessPayload() throws Exception {
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

            mockMvc.perform(post("/project/add")
                            .contentType(APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.msg").value("操作成功"));

            ArgumentCaptor<BizProject> captor = ArgumentCaptor.forClass(BizProject.class);
            verify(bizProjectService).saveProjectWithAttachments(captor.capture(), isNull());
            assertEquals("张三", captor.getValue().getLeaderName());
        }
    }

    /**
     * 作用：验证无权限查看项目详情时会返回 403 业务码，而不是直接暴露数据。
     */
    @Test
    void get_shouldReturnForbiddenWhenProjectIsOutOfScope() throws Exception {
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

            mockMvc.perform(get("/project/get/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(403))
                    .andExpect(jsonPath("$.msg").value("无权限查看该项目"));
        }
    }

    /**
     * 职责：记录项目控制器是否调用了流程服务。
     * 为什么存在：控制器测试只关心有没有正确传参，不需要真正启动流程引擎。
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
