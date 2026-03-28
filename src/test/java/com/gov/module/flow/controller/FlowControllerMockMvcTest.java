package com.gov.module.flow.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gov.module.flow.service.FlowService;
import com.gov.module.project.vo.FlowTaskVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 职责：验证审批控制器在 Web 层的响应契约。
 * 为什么存在：审批中心分页和审批动作是高风险链路，
 * 这里用 MockMvc 锁住返回结构和参数校验语义。
 * 关键输入输出：输入为分页查询参数和审批 JSON，请求结果统一包在 `R` 中。
 * 关联链路：审批中心待办分页、审批通过、审批驳回。
 */
class FlowControllerMockMvcTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RecordingFlowService flowService = new RecordingFlowService();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FlowController controller = new FlowController();
        ReflectionTestUtils.setField(controller, "flowService", flowService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        flowService.todoPage = null;
        flowService.donePage = null;
        flowService.approveTaskId = null;
        flowService.approveResult = null;
    }

    /**
     * 作用：验证待办分页接口返回的是前端可直接消费的分页结构。
     */
    @Test
    void getTodo_shouldReturnPagedResponse() throws Exception {
        FlowTaskVO task = new FlowTaskVO();
        task.setTaskId("task-1");
        task.setProjectName("河道治理项目");

        Page<FlowTaskVO> page = new Page<>(2, 20, 1);
        page.setRecords(Collections.singletonList(task));
        flowService.todoPage = page;

        mockMvc.perform(get("/flow/todo?pageNum=2&pageSize=20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].taskId").value("task-1"))
                .andExpect(jsonPath("$.data.records[0].projectName").value("河道治理项目"));
    }

    /**
     * 作用：验证审批动作在缺少必要参数时会返回业务失败，而不是抛出未处理异常。
     */
    @Test
    void approve_shouldRejectEmptyPayload() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", "task-1");

        mockMvc.perform(post("/flow/approve")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("审批参数不能为空"));
    }

    /**
     * 作用：验证审批动作成功时会透传给服务层，并保持统一响应语义。
     */
    @Test
    void approve_shouldCallServiceWhenPayloadIsValid() throws Exception {
        Map<String, Object> payload = new HashMap<>();
        payload.put("taskId", "task-9");
        payload.put("approved", true);

        mockMvc.perform(post("/flow/approve")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("操作成功"))
                .andExpect(jsonPath("$.data").value("审批操作成功"));

        assertEquals("task-9", flowService.approveTaskId);
        assertEquals(Boolean.TRUE, flowService.approveResult);
    }

    /**
     * 职责：在不依赖 Mockito 增强具体类的前提下记录调用结果。
     * 为什么存在：这样可以把测试关注点留在控制器契约，而不是字节码增强兼容性上。
     */
    static class RecordingFlowService extends FlowService {
        private IPage<FlowTaskVO> todoPage;
        private IPage<FlowTaskVO> donePage;
        private String approveTaskId;
        private Boolean approveResult;

        @Override
        public IPage<FlowTaskVO> getTodoPage(Integer pageNum, Integer pageSize) {
            return todoPage;
        }

        @Override
        public IPage<FlowTaskVO> getDonePage(Integer pageNum, Integer pageSize) {
            return donePage;
        }

        @Override
        public void approve(String taskId, boolean approved) {
            this.approveTaskId = taskId;
            this.approveResult = approved;
        }
    }
}
