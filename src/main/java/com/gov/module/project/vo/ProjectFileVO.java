package com.gov.module.project.vo;

import lombok.Data;

/**
 * 项目附件响应 VO。
 * 详情、上传成功回填等场景统一使用这一结构，
 * 减少前后端对附件字段的重复适配。
 */
@Data
public class ProjectFileVO {
    private Long id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Boolean image;
    private String previewUrl;
    private String downloadUrl;
    private String accessUrl;
}
