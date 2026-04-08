package com.gov.module.system.dto;

import lombok.Data;

import javax.validation.constraints.Size;

/**
 * 角色新增请求 DTO。
 */
@Data
public class RoleCreateDTO {

    @Size(min = 2, max = 50, message = "角色名称长度应为2到50个字符")
    private String roleName;

    @Size(max = 1024, message = "菜单权限长度不能超过1024个字符")
    private String menuPerms;
}
