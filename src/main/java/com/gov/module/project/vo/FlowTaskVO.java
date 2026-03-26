package com.gov.module.project.vo;

import lombok.Data;
import java.util.Date;

@Data
public class FlowTaskVO {
    private String taskId;          // 流程任务ID
    private String taskName;        // 任务名称 (如：部门负责人审批)
    private String processInstanceId; // 流程实例ID
    private String businessKey;     // 业务ID (我们的工程项目ID)

    // 关联的业务数据
    private String projectName;     // 项目名称
    private String leaderName;      // 项目负责人
    private Date createTime;        // 任务到达时间
}