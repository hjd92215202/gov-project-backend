package com.gov.module.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.vo.UserAccessContext;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface SysUserService extends IService<SysUser> {
    String login(String username, String password);

    void logout();

    List<String> getRoleCodes(Long userId);

    List<String> getMenuKeys(Long userId);

    List<Long> getRoleIds(Long userId);

    void assignRoles(Long userId, List<Long> roleIds);

    boolean isAdmin(Long userId);

    boolean isDeptLeader(Long userId);

    UserAccessContext getAccessContext(Long userId);

    Map<Long, List<Long>> getRoleIdsMap(Collection<Long> userIds);
}
