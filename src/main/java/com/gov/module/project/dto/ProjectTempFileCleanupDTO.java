package com.gov.module.project.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 临时附件清理请求 DTO。
 */
@Data
public class ProjectTempFileCleanupDTO {

    @Size(max = 100, message = "待清理附件数量不能超过100个")
    private List<Long> fileIds;
}
