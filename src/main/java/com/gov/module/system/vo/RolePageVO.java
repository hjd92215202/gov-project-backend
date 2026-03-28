package com.gov.module.system.vo;

import lombok.Data;

import java.util.Date;

/**
 * 角色分页响应 VO。
 * 供角色管理列表页使用。
 */
@Data
public class RolePageVO {
    private Long id;
    private String roleName;
    private String roleCode;
    private String menuPerms;
    private Date createTime;
}
