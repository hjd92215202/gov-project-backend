package com.gov.module.project.vo;

import lombok.Data;

import java.util.Date;

/**
 * 审批任务响应 VO。
 * 供待办/已办列表使用，把流程任务信息和项目关键信息拼成一个前端可直接展示的结构。
 */
@Data
public class FlowTaskVO {
    /** 流程任务 ID。 */
    private String taskId;
    /** 任务名称，例如“部门负责人审批”。 */
    private String taskName;
    /** 流程实例 ID。 */
    private String processInstanceId;
    /** 业务主键，这里通常是项目 ID。 */
    private String businessKey;
    /** 关联项目名称。 */
    private String projectName;
    /** 关联项目负责人。 */
    private String leaderName;
    /** 任务创建时间。 */
    private Date createTime;
}
