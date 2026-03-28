package com.gov.module.system.vo;

import lombok.Data;

/**
 * 角色选项响应 VO。
 * 主要用于弹窗里的角色下拉和角色分配场景。
 */
@Data
public class RoleOptionVO {
    private Long id;
    private String roleName;
    private String roleCode;
    private String menuPerms;
}
