package com.gov.module.project.dto;

import lombok.Data;

/**
 * 项目附件提交 DTO。
 * 当前主要用来承接前端已上传附件的 ID 列表，
 * 便于在项目保存时把临时文件正式绑定到项目上。
 */
@Data
public class ProjectAttachmentDTO {
    private Long id;
}
