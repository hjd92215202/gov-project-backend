package com.gov.module.system.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 职责：承载当前用户在一次请求内可复用的权限快照。
 * 为什么存在：把角色、菜单、部门和身份判断结果集中缓存，避免同一请求反复查库。
 * 关键输入输出：输入为当前 userId，输出为角色ID/角色编码/菜单键/部门与身份信息。
 * 关联链路：登录、/me、项目数据范围、审批权限、系统管理权限判断。
 */
@Data
public class UserAccessContext {
    /** 当前用户 ID。 */
    private Long userId;
    /** 当前用户名。 */
    private String username;
    /** 当前用户姓名。 */
    private String realName;
    /** 所属部门 ID。 */
    private Long deptId;
    /** 所属部门名称。 */
    private String deptName;
    /** 绑定的角色 ID 列表。 */
    private List<Long> roleIds = new ArrayList<Long>();
    /** 标准化后的角色编码列表。 */
    private List<String> roleCodes = new ArrayList<String>();
    /** 当前用户最终可访问的菜单键列表。 */
    private List<String> menuKeys = new ArrayList<String>();
    /** 是否具备管理员语义。 */
    private boolean admin;
    /** 是否具备部门负责人语义。 */
    private boolean deptLeader;
}
