package com.gov.module.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户新增请求 DTO。
 * 专门用于创建用户，包含新增场景才会出现的用户名和初始密码。
 */
@Data
public class UserCreateDTO {
    private String username;
    private String password;
    private String realName;
    private Long deptId;
    private String phone;
    private Integer status;
    private List<Long> roleIds;
}
