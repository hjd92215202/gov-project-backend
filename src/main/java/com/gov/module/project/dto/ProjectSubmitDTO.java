package com.gov.module.project.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 项目提交审批请求 DTO。
 * 当前以 `id` 为主，但保留兼容字段，兼顾历史前端调用和新接口语义。
 */
@Data
public class ProjectSubmitDTO {
    private Long id;
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
    private Long creatorId;
    private Long creatorDeptId;
}
