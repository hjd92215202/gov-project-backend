package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.gov.common.result.R;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.UserAccessContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "登录与当前用户")
@RestController
@RequestMapping("/system")
public class LoginController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

    /**
     * 处理用户登录。
     * 该接口负责校验用户名密码、创建登录态，并返回前端初始化所需的用户与权限信息。
     *
     * @param loginBody 登录请求体
     * @return token 与当前用户基础信息
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> loginBody) {
        String token = sysUserService.login(loginBody.get("username"), loginBody.get("password"));
        return R.ok(buildCurrentUserPayload(StpUtil.getLoginIdAsLong(), token), "登录成功");
    }

    @ApiOperation("获取当前用户信息")
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        return R.ok(buildCurrentUserPayload(StpUtil.getLoginIdAsLong(), null));
    }

    /**
     * 退出当前登录会话。
     *
     * @return 中文退出结果
     */
    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public R<String> logout() {
        sysUserService.logout();
        return R.ok("退出登录成功");
    }

    /**
     * 组装前端初始化所需的当前用户载荷。
     * 这里统一把 token、用户基础信息、角色码和菜单权限打包返回，
     * 避免前端再拆多次接口取初始化数据。
     *
     * @param userId 当前用户 ID
     * @param tokenValue 可选 token 值，登录时传入，/me 时传 null
     * @return 当前用户载荷
     */
    private Map<String, Object> buildCurrentUserPayload(Long userId, String tokenValue) {
        UserAccessContext accessContext = sysUserService.getAccessContext(userId);
        SysUser user = sysUserService.getById(userId);
        SysDept dept = accessContext.getDeptId() == null ? null : sysDeptService.getById(accessContext.getDeptId());

        Map<String, Object> result = new HashMap<>();
        if (tokenValue != null) {
            result.put("tokenValue", tokenValue);
            result.put("tokenName", "Authorization");
        }
        result.put("userId", userId);
        result.put("username", user == null ? null : user.getUsername());
        result.put("realName", user == null ? null : user.getRealName());
        result.put("deptId", accessContext.getDeptId());
        result.put("deptName", dept == null ? null : dept.getDeptName());
        result.put("roleCodes", accessContext.getRoleCodes());
        result.put("menuKeys", accessContext.getMenuKeys());
        return result;
    }
}
