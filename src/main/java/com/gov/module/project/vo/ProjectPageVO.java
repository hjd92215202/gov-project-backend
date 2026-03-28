package com.gov.module.project.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目分页响应 VO。
 * 专门给项目列表页使用，只返回表格真正需要的字段，避免把实体整包透出。
 */
@Data
public class ProjectPageVO {
    private Long id;
    private String projectName;
    private String projectCode;
    private String address;
    private String province;
    private String city;
    private String district;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String leaderName;
    private String leaderPhone;
    private Integer status;
    private Long creatorId;
    private Long creatorDeptId;
    private Date createTime;
}
