package com.gov.config;

import cn.dev33.satoken.stp.StpInterface;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义权限验证接口扩展
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysUserService sysUserService;

    /** 返回一个账号所拥有的权限码集合 */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 暂时返回空，后续对接 sys_menu 表
        return new ArrayList<>();
    }

    /** 返回一个账号所拥有的角色标识集合 */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 这里为了演示，如果是 ID 为 1 的用户，直接给 admin 角色
        // 以后我们要在这里查 sys_user_role 表
        List<String> list = new ArrayList<>();
        if ("1".equals(String.valueOf(loginId))) {
            list.add("admin");
        }
        return list;
    }
}