package com.gov.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.module.system.entity.SysUser;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    /**
     * 用户登录。
     *
     * @return 登录后的 Token
     */
    String login(String username, String password);

    /**
     * 用户登出。
     */
    void logout();

    /**
     * 获取用户角色编码。
     */
    List<String> getRoleCodes(Long userId);

    /**
     * 根据角色合并可访问菜单权限键。
     */
    List<String> getMenuKeys(Long userId);

    /**
     * 获取用户角色 ID 列表。
     */
    List<Long> getRoleIds(Long userId);

    /**
     * 覆盖设置用户角色。
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 是否管理员。
     */
    boolean isAdmin(Long userId);

    /**
     * 是否部门负责人。
     */
    boolean isDeptLeader(Long userId);
}
