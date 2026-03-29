package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.common.result.R;
import com.gov.module.system.entity.SysAuditLog;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysAuditLogService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.AuditLogPageVO;
import com.gov.module.system.vo.UserAccessContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 职责：提供系统审计日志分页查询接口。
 * 为什么存在：满足“超级管理员可追溯谁在什么时间做了什么请求”的审计诉求。
 * 关键输入输出：输入为筛选条件与分页参数，输出为审计日志分页 VO。
 * 关联链路：系统管理 -> 审计日志页面。
 */
@Api(tags = "审计日志管理")
@RestController
@RequestMapping("/system/audit")
public class SysAuditController {

    @Autowired
    private SysAuditLogService sysAuditLogService;

    @Autowired
    private SysUserService sysUserService;

    /**
     * 分页查询审计日志（仅超级管理员可访问）。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @param userId 用户 ID
     * @param keyword 用户关键字（用户名/真实姓名）
     * @param requestMethod HTTP 方法
     * @param requestUri 请求路径关键字
     * @param httpStatus HTTP 状态码
     * @param clientIp 客户端 IP 关键字
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 审计日志分页结果
     */
    @ApiOperation("审计日志分页查询")
    @GetMapping("/page")
    public R<IPage<AuditLogPageVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String requestMethod,
            @RequestParam(required = false) String requestUri,
            @RequestParam(required = false) Integer httpStatus,
            @RequestParam(required = false) String clientIp,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime
    ) {
        UserAccessContext accessContext = sysUserService.getAccessContext(StpUtil.getLoginIdAsLong());
        if (!accessContext.isAdmin()) {
            return R.fail(403, "仅超级管理员可查看审计日志");
        }

        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(userId != null, SysAuditLog::getUserId, userId);
        wrapper.eq(StrUtil.isNotBlank(requestMethod), SysAuditLog::getRequestMethod,
                requestMethod == null ? null : requestMethod.trim().toUpperCase());
        wrapper.like(StrUtil.isNotBlank(requestUri), SysAuditLog::getRequestUri, requestUri == null ? null : requestUri.trim());
        wrapper.eq(httpStatus != null, SysAuditLog::getHttpStatus, httpStatus);
        wrapper.like(StrUtil.isNotBlank(clientIp), SysAuditLog::getClientIp, clientIp == null ? null : clientIp.trim());
        wrapper.ge(startTime != null, SysAuditLog::getRequestTime, startTime);
        wrapper.le(endTime != null, SysAuditLog::getRequestTime, endTime);

        if (StrUtil.isNotBlank(keyword)) {
            List<Long> matchedUserIds = loadUserIdsByKeyword(keyword.trim());
            if (matchedUserIds.isEmpty()) {
                return R.ok(buildPageResult(pageNum, pageSize, 0L, Collections.emptyList()));
            }
            wrapper.in(SysAuditLog::getUserId, matchedUserIds);
        }

        wrapper.orderByDesc(SysAuditLog::getRequestTime).orderByDesc(SysAuditLog::getId);
        IPage<SysAuditLog> page;
        try {
            page = sysAuditLogService.page(new Page<>(pageNum, pageSize), wrapper);
        } catch (Exception error) {
            return R.fail("审计日志表尚未初始化，请重启后端服务后重试");
        }
        List<AuditLogPageVO> records = toAuditLogVO(page.getRecords());
        return R.ok(buildPageResult(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    private List<Long> loadUserIdsByKeyword(String keyword) {
        if (StrUtil.isBlank(keyword)) {
            return Collections.emptyList();
        }
        List<SysUser> users = sysUserService.list(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId, SysUser::getUsername, SysUser::getRealName)
                .and(wrapper -> wrapper
                        .like(SysUser::getUsername, keyword)
                        .or()
                        .like(SysUser::getRealName, keyword)));
        return users.stream()
                .map(SysUser::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<AuditLogPageVO> toAuditLogVO(List<SysAuditLog> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        Set<Long> userIds = records.stream()
                .map(SysAuditLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = new HashMap<>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserService.list(new LambdaQueryWrapper<SysUser>()
                    .select(SysUser::getId, SysUser::getUsername, SysUser::getRealName)
                    .in(SysUser::getId, userIds));
            for (SysUser user : users) {
                if (user != null && user.getId() != null) {
                    userMap.put(user.getId(), user);
                }
            }
        }

        List<AuditLogPageVO> result = new ArrayList<>();
        for (SysAuditLog item : records) {
            AuditLogPageVO vo = new AuditLogPageVO();
            vo.setId(item.getId());
            vo.setUserId(item.getUserId());
            SysUser user = item.getUserId() == null ? null : userMap.get(item.getUserId());
            vo.setUsername(user == null ? null : user.getUsername());
            vo.setRealName(user == null ? null : user.getRealName());
            vo.setRequestMethod(item.getRequestMethod());
            vo.setRequestUri(item.getRequestUri());
            vo.setClientIp(item.getClientIp());
            vo.setHttpStatus(item.getHttpStatus());
            vo.setDurationMs(item.getDurationMs());
            vo.setTraceId(item.getTraceId());
            vo.setUserAgent(item.getUserAgent());
            vo.setRequestTime(item.getRequestTime());
            result.add(vo);
        }
        return result;
    }

    private IPage<AuditLogPageVO> buildPageResult(long pageNum, long pageSize, long total, List<AuditLogPageVO> records) {
        Page<AuditLogPageVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(records == null ? new ArrayList<>() : records);
        return page;
    }
}
