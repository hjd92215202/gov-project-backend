package com.gov.module.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 角色菜单更新请求 DTO。
 * 用于把树形菜单勾选结果转换成后端存储的菜单权限串。
 */
@Data
public class RoleMenuUpdateDTO {
    private List<String> menuKeys;
    private String menuPerms;
}
