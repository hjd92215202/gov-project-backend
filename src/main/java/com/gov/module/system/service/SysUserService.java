package com.gov.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.module.system.entity.SysUser;

import java.util.List;

public interface SysUserService extends IService<SysUser> {
    /**
     * 用户登录逻辑
     * @return 返回生成的 Token
     */
    String login(String username, String password);

    /**
     * 用户注销逻辑
     */
    void logout();

    /**
     * 获取用户角色编码
     */
    List<String> getRoleCodes(Long userId);

    /**
     * 获取用户角色ID列表
     */
    List<Long> getRoleIds(Long userId);

    /**
     * 覆盖设置用户角色
     */
    void assignRoles(Long userId, List<Long> roleIds);

    /**
     * 是否管理员
     */
    boolean isAdmin(Long userId);

    /**
     * 是否部门负责人角色
     */
    boolean isDeptLeader(Long userId);
}
