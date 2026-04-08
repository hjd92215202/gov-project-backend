package com.gov.module.flow.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class FlowApproveDTO {

    @NotBlank(message = "审批任务ID不能为空")
    @Size(max = 64, message = "审批任务ID长度不能超过64位")
    private String taskId;

    @NotNull(message = "审批结果不能为空")
    private Boolean approved;
}
