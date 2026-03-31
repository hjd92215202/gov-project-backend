package com.gov.module.project.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目详情响应 VO。
 * 给新增/编辑弹窗和详情查看使用，字段比列表更全，但仍然比实体更收敛。
 */
@Data
public class ProjectDetailVO {
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
    private Long creatorDeptId;
    private List<ProjectFileVO> attachments = new ArrayList<>();
}
