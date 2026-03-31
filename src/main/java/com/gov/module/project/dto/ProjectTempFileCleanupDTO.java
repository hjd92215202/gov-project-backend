package com.gov.module.project.dto;

import lombok.Data;

import java.util.List;

/**
 * 临时附件清理请求 DTO。
 * 用于在用户取消编辑时回收当前会话中新上传但未绑定项目的附件。
 */
@Data
public class ProjectTempFileCleanupDTO {
    private List<Long> fileIds;
}
