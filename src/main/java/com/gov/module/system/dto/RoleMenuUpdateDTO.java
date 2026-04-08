package com.gov.module.system.dto;

import lombok.Data;

import javax.validation.constraints.Size;
import java.util.List;

/**
 * 角色菜单更新请求 DTO。
 */
@Data
public class RoleMenuUpdateDTO {

    @Size(max = 50, message = "菜单权限数量不能超过50个")
    private List<String> menuKeys;

    @Size(max = 1024, message = "菜单权限长度不能超过1024个字符")
    private String menuPerms;
}
