package com.gov.config;

import cn.dev33.satoken.stp.StpInterface;
import com.gov.module.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 权限与角色回调实现。
 * 这个类存在的意义是把 Sa-Token 的“角色查询 / 权限查询”桥接到当前业务系统，
 * 让框架在做角色判断时能够回调到我们的用户权限模型。
 */
@Component
public class StpInterfaceImpl implements StpInterface {

    @Autowired
    private SysUserService sysUserService;

    /**
     * 返回账号拥有的权限码集合。
     * 当前项目主要以菜单键控制前端页面，因此这里暂未对接细粒度权限码表。
     *
     * @param loginId 当前登录账号 ID
     * @param loginType 登录体系标识
     * @return 权限码列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 暂时返回空，后续对接 sys_menu 表
        return new ArrayList<>();
    }

    /**
     * 返回账号拥有的角色标识集合。
     * 角色数据最终来自 `SysUserService` 的统一访问上下文计算结果。
     *
     * @param loginId 当前登录账号 ID
     * @param loginType 登录体系标识
     * @return 角色标识列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        try {
            Long userId = Long.parseLong(String.valueOf(loginId));
            return sysUserService.getRoleCodes(userId);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
