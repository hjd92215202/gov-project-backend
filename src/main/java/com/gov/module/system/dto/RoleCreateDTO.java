package com.gov.module.system.dto;

import lombok.Data;

/**
 * 角色新增请求 DTO。
 */
@Data
public class RoleCreateDTO {
    private String roleName;
    private String menuPerms;
}
