package com.gov.module.system.dto;

import lombok.Data;

/**
 * 角色更新请求 DTO。
 */
@Data
public class RoleUpdateDTO {
    private Long id;
    private String roleName;
    private String menuPerms;
}
