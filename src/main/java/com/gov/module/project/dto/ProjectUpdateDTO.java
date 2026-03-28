package com.gov.module.project.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 项目更新请求 DTO。
 * 和新增 DTO 分开，是为了把“必须携带 id”的更新语义表达清楚。
 */
@Data
public class ProjectUpdateDTO {
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
    private Integer status;
    private Long creatorId;
    private Long creatorDeptId;
}
