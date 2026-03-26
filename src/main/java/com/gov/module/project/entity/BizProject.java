package com.gov.module.project.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gov.crypto.SmTypeHandler;
import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
@TableName(value = "biz_project", autoResultMap = true) // 必须开启 autoResultMap 才能解密
public class BizProject {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String projectName;
    private String projectCode;
    private String address;

    private String province; // 省份
    private String city;     // 城市
    private String district; // 区县

    private BigDecimal longitude; // 经度
    private BigDecimal latitude;  // 纬度

    private String leaderName;

    /**
     * 关键点：使用国密 SM4 加密手机号
     * 数据库里存的是密文，MyBatis 查询出来自动变成明文
     */
    @TableField(typeHandler = SmTypeHandler.class)
    private String leaderPhone;

    private String description;
    private Integer status; // 0待提交, 1审批中, 2已通过, 3被驳回

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE) // 如果刚才 SQL 加了 ON UPDATE，这里也可以加上
    private Date updateTime;
}