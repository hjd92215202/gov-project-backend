package com.gov.module.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户访问上下文。
 * 它是一次性聚合出来的权限快照，集中保存角色、菜单、部门和身份判断结果，
 * 让控制器和服务在一次请求里不用反复查询这些高频信息。
 */
@Data
public class UserAccessContext {
    /** 当前用户 ID。 */
    private Long userId;
    /** 所属部门 ID。 */
    private Long deptId;
    /** 绑定的角色 ID 列表。 */
    private List<Long> roleIds = new ArrayList<>();
    /** 标准化后的角色编码列表。 */
    private List<String> roleCodes = new ArrayList<>();
    /** 当前用户最终可访问的菜单键列表。 */
    private List<String> menuKeys = new ArrayList<>();
    /** 是否具备管理员语义。 */
    private boolean admin;
    /** 是否具备部门负责人语义。 */
    private boolean deptLeader;
}
