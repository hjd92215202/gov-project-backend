package com.gov.module.project.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 项目附件提交 DTO。
 */
@Data
public class ProjectAttachmentDTO {

    @NotNull(message = "附件ID不能为空")
    private Long id;
}
