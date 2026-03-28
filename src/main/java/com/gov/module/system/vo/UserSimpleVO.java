package com.gov.module.system.vo;

import lombok.Data;

/**
 * 用户简表响应 VO。
 * 给下拉选择等轻量场景使用，避免拉取完整用户列表结构。
 */
@Data
public class UserSimpleVO {
    private Long id;
    private Long deptId;
    private String username;
    private String realName;
    private String phone;
    private Integer status;
}
