package com.gov.module.project.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 项目新增请求 DTO。
 * 只描述“创建项目”这件事允许前端传入的字段，避免直接复用数据库实体。
 */
@Data
public class ProjectCreateDTO {
    private String projectName;
    private String projectCode;
    private String address;
    private String province;
    private String city;
    private String district;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private Long leaderUserId;
    private String leaderName;
    private String leaderPhone;
    private String description;
    private Integer status;
    private Long creatorId;
    private Long creatorDeptId;
    private List<ProjectAttachmentDTO> attachments;
}
