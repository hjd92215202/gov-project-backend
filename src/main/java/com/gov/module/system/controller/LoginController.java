package com.gov.module.system.controller;

import com.gov.common.result.R;
import com.gov.module.system.service.SysUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "用户登录管理")
@RestController
@RequestMapping("/system")
public class LoginController {

    @Autowired
    private SysUserService sysUserService;

    @ApiOperation("登录接口")
    @PostMapping("/login")
    public R<Map<String, String>> login(@RequestBody Map<String, String> loginBody) {
        String username = loginBody.get("username");
        String password = loginBody.get("password");

        // 调用登录业务，返回 Token
        String token = sysUserService.login(username, password);

        Map<String, String> result = new HashMap<>();
        result.put("tokenValue", token);
        result.put("tokenName", "Authorization");

        return R.ok(result, "登录成功");
    }

    @ApiOperation("注销接口")
    @PostMapping("/logout")
    public R<String> logout() {
        // 注销当前会话
//        sysUserService.loginOut(); // 后续补充此方法或直接StpUtil.logout()
        return R.ok("注销成功");
    }
}