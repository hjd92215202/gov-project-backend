package com.gov.module.project.dto;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Max;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

import static com.gov.common.validation.ValidationPatterns.PHONE;
import static com.gov.common.validation.ValidationPatterns.PROJECT_CODE;

/**
 * 项目新增请求 DTO。
 */
@Data
public class ProjectCreateDTO {

    @Size(max = 100, message = "项目名称长度不能超过100个字符")
    private String projectName;

    @Pattern(regexp = "^$|" + PROJECT_CODE, message = "项目编号格式不正确")
    private String projectCode;

    @Size(max = 255, message = "项目地址长度不能超过255个字符")
    private String address;

    @Size(max = 32, message = "省份长度不能超过32个字符")
    private String province;

    @Size(max = 32, message = "城市长度不能超过32个字符")
    private String city;

    @Size(max = 32, message = "区县长度不能超过32个字符")
    private String district;

    @DecimalMin(value = "-180.0", message = "经度不能小于-180")
    @DecimalMax(value = "180.0", message = "经度不能大于180")
    private BigDecimal longitude;

    @DecimalMin(value = "-90.0", message = "纬度不能小于-90")
    @DecimalMax(value = "90.0", message = "纬度不能大于90")
    private BigDecimal latitude;

    private Long leaderUserId;

    @Size(max = 30, message = "负责人姓名长度不能超过30个字符")
    private String leaderName;

    @Pattern(regexp = "^$|" + PHONE, message = "负责人手机号格式不正确")
    private String leaderPhone;

    @Size(max = 2000, message = "项目描述长度不能超过2000个字符")
    private String description;

    @Max(value = 3, message = "项目状态值不合法")
    private Integer status;

    private Long creatorId;
    private Long creatorDeptId;

    @Valid
    @Size(max = 100, message = "附件数量不能超过100个")
    private List<ProjectAttachmentDTO> attachments;
}
