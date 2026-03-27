package com.gov.module.project.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gov.crypto.SmTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName(value = "biz_project", autoResultMap = true)
public class BizProject {

    @TableId(type = IdType.ASSIGN_ID)
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

    @TableField(typeHandler = SmTypeHandler.class)
    private String leaderPhone;
    @TableField(exist = false)
    private Long leaderUserId;

    private String description;
    private Integer status;
    private Long creatorId;
    private Long creatorDeptId;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
