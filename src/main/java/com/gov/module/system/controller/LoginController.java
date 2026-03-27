package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.gov.common.result.R;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(tags = "用户登录管理")
@RestController
@RequestMapping("/system")
public class LoginController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

    @ApiOperation("登录接口")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> loginBody) {
        String username = loginBody.get("username");
        String password = loginBody.get("password");

        // 调用登录业务，返回 Token
        String token = sysUserService.login(username, password);
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(userId);
        List<String> roleCodes = sysUserService.getRoleCodes(userId);
        SysDept dept = user == null ? null : sysDeptService.getById(user.getDeptId());

        Map<String, Object> result = new HashMap<>();
        result.put("tokenValue", token);
        result.put("tokenName", "Authorization");
        result.put("userId", userId);
        result.put("username", user == null ? null : user.getUsername());
        result.put("realName", user == null ? null : user.getRealName());
        result.put("deptId", user == null ? null : user.getDeptId());
        result.put("deptName", dept == null ? null : dept.getDeptName());
        result.put("roleCodes", roleCodes == null ? new ArrayList<String>() : roleCodes);

        return R.ok(result, "登录成功");
    }

    @ApiOperation("获取当前登录用户信息")
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = sysUserService.getById(userId);
        List<String> roleCodes = sysUserService.getRoleCodes(userId);
        SysDept dept = user == null ? null : sysDeptService.getById(user.getDeptId());

        Map<String, Object> result = new HashMap<>();
        result.put("userId", userId);
        result.put("username", user == null ? null : user.getUsername());
        result.put("realName", user == null ? null : user.getRealName());
        result.put("deptId", user == null ? null : user.getDeptId());
        result.put("deptName", dept == null ? null : dept.getDeptName());
        result.put("roleCodes", roleCodes == null ? new ArrayList<String>() : roleCodes);
        return R.ok(result);
    }

    @ApiOperation("注销接口")
    @PostMapping("/logout")
    public R<String> logout() {
        sysUserService.logout();
        return R.ok("注销成功");
    }
}
