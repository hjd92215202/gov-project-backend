package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.gov.common.result.R;
import com.gov.common.security.CsrfTokenManager;
import com.gov.module.system.dto.LoginDTO;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.UserAccessContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@Api(tags = "登录与当前用户")
@RestController
@RequestMapping("/system")
@Validated
public class LoginController {

    private static final Logger perfLog = LoggerFactory.getLogger("com.gov.perf");

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

    @Autowired(required = false)
    private CsrfTokenManager csrfTokenManager;

    @ApiOperation("用户登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@Valid @RequestBody LoginDTO loginBody,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        long startAt = System.currentTimeMillis();
        String username = loginBody.getUsername();
        sysUserService.login(username, loginBody.getPassword());
        Long userId = StpUtil.getLoginIdAsLong();
        if (csrfTokenManager != null && response != null) {
            csrfTokenManager.ensureToken(request, response);
        }
        Map<String, Object> payload = buildCurrentUserPayload(userId);
        perfLog.info("action=login userId={} username={} durationMs={}",
                userId, username, System.currentTimeMillis() - startAt);
        return R.ok(payload, "登录成功");
    }

    @ApiOperation("获取当前用户信息")
    @GetMapping("/me")
    public R<Map<String, Object>> me(HttpServletRequest request, HttpServletResponse response) {
        long startAt = System.currentTimeMillis();
        Long userId = StpUtil.getLoginIdAsLong();
        if (csrfTokenManager != null && response != null) {
            csrfTokenManager.ensureToken(request, response);
        }
        Map<String, Object> payload = buildCurrentUserPayload(userId);
        perfLog.info("action=me userId={} durationMs={}", userId, System.currentTimeMillis() - startAt);
        return R.ok(payload);
    }

    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public R<String> logout(HttpServletResponse response) {
        sysUserService.logout();
        if (csrfTokenManager != null && response != null) {
            csrfTokenManager.clearToken(response);
        }
        return R.ok("退出登录成功");
    }

    public R<Map<String, Object>> login(LoginDTO loginBody) {
        return login(loginBody, null, null);
    }

    public R<Map<String, Object>> me() {
        return me(null, null);
    }

    private Map<String, Object> buildCurrentUserPayload(Long userId) {
        UserAccessContext accessContext = sysUserService.getAccessContext(userId);
        String username = accessContext.getUsername();
        String realName = accessContext.getRealName();
        String deptName = accessContext.getDeptName();

        if (username == null || realName == null) {
            SysUser user = sysUserService.getById(userId);
            if (user != null) {
                if (username == null) {
                    username = user.getUsername();
                }
                if (realName == null) {
                    realName = user.getRealName();
                }
            }
        }

        if (deptName == null && accessContext.getDeptId() != null) {
            SysDept dept = sysDeptService.getById(accessContext.getDeptId());
            deptName = dept == null ? null : dept.getDeptName();
        }

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("userId", userId);
        result.put("username", username);
        result.put("realName", realName);
        result.put("deptId", accessContext.getDeptId());
        result.put("deptName", deptName);
        result.put("roleCodes", accessContext.getRoleCodes());
        result.put("menuKeys", accessContext.getMenuKeys());
        return result;
    }
}
