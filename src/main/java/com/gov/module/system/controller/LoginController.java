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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Api(tags = "登录与当前用户")
@RestController
@RequestMapping("/system")
public class LoginController {

    private static final Logger perfLog = LoggerFactory.getLogger("com.gov.perf");

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

    /**
     * 职责：处理登录并一次性返回前端初始化所需的用户与权限快照。
     * 为什么存在：避免前端登录成功后再拆成多次接口请求，减少首屏初始化等待。
     * 关键输入输出：输入为用户名与密码，输出为 token、用户基础信息、角色编码和菜单权限。
     * 关联链路：登录页 -> /system/login -> session store -> 菜单与首页解析。
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public R<Map<String, Object>> login(@RequestBody Map<String, String> loginBody) {
        long startAt = System.currentTimeMillis();
        String username = loginBody == null ? null : loginBody.get("username");
        String token = sysUserService.login(username, loginBody == null ? null : loginBody.get("password"));
        Long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> payload = buildCurrentUserPayload(userId, token);
        perfLog.info("action=login userId={} username={} durationMs={}", userId, username, System.currentTimeMillis() - startAt);
        return R.ok(payload, "登录成功");
    }

    /**
     * 职责：返回当前登录用户的会话初始化信息。
     * 为什么存在：前端刷新、切页补拉权限时只需要一次接口即可恢复完整上下文。
     * 关键输入输出：输入为当前登录态，输出为不含 token 的用户、角色、菜单信息。
     * 关联链路：路由守卫 -> /system/me -> session store -> 权限校验。
     */
    @ApiOperation("获取当前用户信息")
    @GetMapping("/me")
    public R<Map<String, Object>> me() {
        long startAt = System.currentTimeMillis();
        Long userId = StpUtil.getLoginIdAsLong();
        Map<String, Object> payload = buildCurrentUserPayload(userId, null);
        perfLog.info("action=me userId={} durationMs={}", userId, System.currentTimeMillis() - startAt);
        return R.ok(payload);
    }

    /**
     * 职责：退出当前登录会话。
     * 为什么存在：统一释放 Sa-Token 登录态，避免前后端各自清理造成状态不一致。
     * 关键输入输出：输入为当前登录态，输出为中文退出结果。
     * 关联链路：右上角退出登录 -> /system/logout -> 登录页。
     */
    @ApiOperation("退出登录")
    @PostMapping("/logout")
    public R<String> logout() {
        sysUserService.logout();
        return R.ok("退出登录成功");
    }

    /**
     * 职责：组装当前用户载荷。
     * 为什么存在：统一封装 token、用户、部门、角色、菜单字段，避免控制器散落重复拼装逻辑。
     * 关键输入输出：输入为 userId 和可选 token，输出为前端可直接消费的会话载荷。
     * 关联链路：/system/login、/system/me。
     */
    private Map<String, Object> buildCurrentUserPayload(Long userId, String tokenValue) {
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
        if (tokenValue != null) {
            result.put("tokenValue", tokenValue);
            result.put("tokenName", "Authorization");
        }
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
