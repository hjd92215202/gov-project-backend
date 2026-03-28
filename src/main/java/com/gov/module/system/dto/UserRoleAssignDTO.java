package com.gov.module.system.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户角色分配请求 DTO。
 * 用于单独维护用户与角色的绑定关系。
 */
@Data
public class UserRoleAssignDTO {
    private Long userId;
    private List<Long> roleIds;
}
