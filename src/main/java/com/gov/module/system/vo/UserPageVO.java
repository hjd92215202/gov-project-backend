package com.gov.module.system.vo;

import lombok.Data;

import java.util.Date;

/**
 * 用户分页响应 VO。
 * 供用户管理列表页使用，只保留页面真正需要展示的字段。
 */
@Data
public class UserPageVO {
    private Long id;
    private Long deptId;
    private String username;
    private String realName;
    private String phone;
    private Integer status;
    private Date createTime;
    private String deptName;
    private String roleNames;
}
