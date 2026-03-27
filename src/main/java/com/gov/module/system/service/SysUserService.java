package com.gov.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.module.system.entity.SysUser;

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
}
