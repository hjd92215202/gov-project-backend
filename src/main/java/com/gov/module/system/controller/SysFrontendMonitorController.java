package com.gov.module.system.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gov.common.result.R;
import com.gov.module.system.dto.FrontendLogItemDTO;
import com.gov.module.system.dto.FrontendLogReportDTO;
import com.gov.module.system.entity.SysDept;
import com.gov.module.system.entity.SysFrontendLog;
import com.gov.module.system.entity.SysUser;
import com.gov.module.system.service.SysDeptService;
import com.gov.module.system.service.SysFrontendLogService;
import com.gov.module.system.service.SysUserService;
import com.gov.module.system.vo.FrontendLogPageVO;
import com.gov.module.system.vo.UserAccessContext;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 职责：提供前端运行监控日志上报与分页查询接口。
 * 为什么存在：把浏览器侧真实运行情况接入后端可检索体系，便于管理员集中定位页面问题。
 * 关键输入输出：输入为前端批量日志 DTO 或分页筛选条件，输出为中文上报结果或分页 VO。
 * 关联链路：logger/front-monitor -> 前端监控页面。
 */
@Api(tags = "前端监控")
@RestController
@RequestMapping("/system/frontend-monitor")
@Validated
public class SysFrontendMonitorController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SysFrontendMonitorController.class);
    private static final Logger perfLog = LoggerFactory.getLogger("com.gov.perf");
    private static final int MAX_BATCH_SIZE = 50;

    @Autowired
    private SysFrontendLogService sysFrontendLogService;

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private SysDeptService sysDeptService;

    /**
     * 职责：接收浏览器侧批量上报的运行监控日志。
     * 为什么存在：前端异常和慢链路只在浏览器出现，必须回传后端后才能供管理员统一分析。
     * 关键输入输出：输入为前端日志批量 DTO，输出为已接收条数。
     * 关联链路：前端 logger/front-monitor。
     */
    @ApiOperation("上报前端监控日志")
    @PostMapping("/report")
    public R<String> report(@Valid @RequestBody(required = false) FrontendLogReportDTO payload, HttpServletRequest request) {
        long startAt = System.currentTimeMillis();
        List<FrontendLogItemDTO> items = payload == null ? null : payload.getLogs();
        if (items == null || items.isEmpty()) {
            return R.ok("未收到有效日志");
        }

        Long userId = resolveCurrentUserId();
        String clientIp = resolveClientIp(request);
        String userAgent = truncate(request.getHeader("User-Agent"), 500);
        List<SysFrontendLog> entities = new ArrayList<SysFrontendLog>();
        for (FrontendLogItemDTO item : items) {
            if (item == null || StrUtil.isBlank(item.getMessage())) {
                continue;
            }
            if (entities.size() >= MAX_BATCH_SIZE) {
                break;
            }
            SysFrontendLog entity = new SysFrontendLog();
            entity.setUserId(userId);
            entity.setLogLevel(truncate(normalizeLowerText(item.getLevel()), 16));
            entity.setLogType(truncate(normalizeLowerText(item.getType()), 32));
            entity.setEventName(truncate(item.getEventName(), 64));
            entity.setMessage(truncate(item.getMessage(), 255));
            entity.setPagePath(truncate(item.getPagePath(), 255));
            entity.setTraceId(truncate(item.getTraceId(), 64));
            entity.setDetailJson(truncate(item.getDetailJson(), 4000));
            entity.setClientIp(truncate(clientIp, 64));
            entity.setUserAgent(userAgent);
            entity.setCreatedTime(parseDate(item.getTime()));
            entities.add(entity);
        }

        if (entities.isEmpty()) {
            return R.ok("未收到有效日志");
        }

        sysFrontendLogService.saveBatch(entities);
        perfLog.info("action=frontendMonitorReport userId={} count={} durationMs={}",
                userId, entities.size(), System.currentTimeMillis() - startAt);
        return R.ok("已接收" + entities.size() + "条前端监控日志");
    }

    /**
     * 职责：分页查询前端运行监控日志。
     * 为什么存在：管理员需要按时间、用户、页面、级别和 traceId 等维度快速排查真实线上问题。
     * 关键输入输出：输入为分页与筛选参数，输出为前端监控分页 VO。
     * 关联链路：系统设置 -> 前端监控页面。
     */
    @ApiOperation("前端监控分页查询")
    @GetMapping("/page")
    public R<IPage<FrontendLogPageVO>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String deptName,
            @RequestParam(required = false) String logLevel,
            @RequestParam(required = false) String logType,
            @RequestParam(required = false) String pagePath,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date endTime
    ) {
        long startAt = System.currentTimeMillis();
        UserAccessContext accessContext = sysUserService.getAccessContext(StpUtil.getLoginIdAsLong());
        if (!accessContext.isAdmin()) {
            return R.fail(403, "仅管理员可查看前端监控");
        }

        QueryWrapper<SysFrontendLog> wrapper = new QueryWrapper<SysFrontendLog>();
        wrapper.select("id", "user_id", "log_level", "log_type", "event_name", "message",
                "page_path", "trace_id", "detail_json", "client_ip", "user_agent", "created_time");
        wrapper.eq(StrUtil.isNotBlank(logLevel), "log_level", normalizeLowerText(logLevel));
        wrapper.eq(StrUtil.isNotBlank(logType), "log_type", normalizeLowerText(logType));
        wrapper.like(StrUtil.isNotBlank(pagePath), "page_path", pagePath == null ? null : pagePath.trim());
        wrapper.like(StrUtil.isNotBlank(traceId), "trace_id", traceId == null ? null : traceId.trim());
        wrapper.ge(startTime != null, "created_time", startTime);
        wrapper.le(endTime != null, "created_time", endTime);

        Set<Long> deptMatchedUserIds = loadUserIdsByDept(deptName);
        if (deptMatchedUserIds != null) {
            if (deptMatchedUserIds.isEmpty()) {
                return R.ok(buildPageResult(pageNum, pageSize, 0L, Collections.<FrontendLogPageVO>emptyList()));
            }
            wrapper.in("user_id", deptMatchedUserIds);
        }

        if (StrUtil.isNotBlank(keyword)) {
            String trimmedKeyword = keyword.trim();
            List<Long> keywordUserIds = loadUserIdsByKeyword(trimmedKeyword);
            wrapper.and(query -> query
                    .like("message", trimmedKeyword)
                    .or()
                    .like("event_name", trimmedKeyword)
                    .or()
                    .like("page_path", trimmedKeyword)
                    .or()
                    .like("trace_id", trimmedKeyword)
                    .or(keywordUserIds != null && !keywordUserIds.isEmpty())
                    .in(keywordUserIds != null && !keywordUserIds.isEmpty(), "user_id", keywordUserIds));
        }

        wrapper.orderByDesc("created_time").orderByDesc("id");
        IPage<SysFrontendLog> page = sysFrontendLogService.page(new Page<SysFrontendLog>(pageNum, pageSize), wrapper);
        List<FrontendLogPageVO> records = toFrontendLogVO(page.getRecords());
        perfLog.info("action=frontendMonitorPage operatorUserId={} pageNum={} pageSize={} total={} durationMs={}",
                accessContext.getUserId(), pageNum, pageSize, page.getTotal(), System.currentTimeMillis() - startAt);
        return R.ok(buildPageResult(page.getCurrent(), page.getSize(), page.getTotal(), records));
    }

    private List<FrontendLogPageVO> toFrontendLogVO(List<SysFrontendLog> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> userIds = records.stream()
                .map(SysFrontendLog::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, SysUser> userMap = new HashMap<Long, SysUser>();
        if (!userIds.isEmpty()) {
            List<SysUser> users = sysUserService.list(new LambdaQueryWrapper<SysUser>()
                    .select(SysUser::getId, SysUser::getUsername, SysUser::getRealName, SysUser::getDeptId)
                    .in(SysUser::getId, userIds));
            for (SysUser user : users) {
                if (user != null && user.getId() != null) {
                    userMap.put(user.getId(), user);
                }
            }
        }

        Set<Long> deptIds = userMap.values().stream()
                .map(SysUser::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = new HashMap<Long, String>();
        if (!deptIds.isEmpty()) {
            List<SysDept> deptList = sysDeptService.listByIds(deptIds);
            for (SysDept dept : deptList) {
                if (dept != null && dept.getId() != null) {
                    deptNameMap.put(dept.getId(), dept.getDeptName());
                }
            }
        }

        List<FrontendLogPageVO> result = new ArrayList<FrontendLogPageVO>();
        for (SysFrontendLog item : records) {
            FrontendLogPageVO vo = new FrontendLogPageVO();
            vo.setId(item.getId());
            vo.setUserId(item.getUserId());
            SysUser user = item.getUserId() == null ? null : userMap.get(item.getUserId());
            vo.setUsername(user == null ? null : user.getUsername());
            vo.setRealName(user == null ? null : user.getRealName());
            vo.setDeptName(user == null ? null : deptNameMap.get(user.getDeptId()));
            vo.setLogLevel(item.getLogLevel());
            vo.setLogType(item.getLogType());
            vo.setEventName(item.getEventName());
            vo.setMessage(item.getMessage());
            vo.setPagePath(item.getPagePath());
            vo.setTraceId(item.getTraceId());
            vo.setDetailJson(item.getDetailJson());
            vo.setClientIp(item.getClientIp());
            vo.setUserAgent(item.getUserAgent());
            vo.setCreatedTime(item.getCreatedTime());
            result.add(vo);
        }
        return result;
    }

    private IPage<FrontendLogPageVO> buildPageResult(long pageNum, long pageSize, long total, List<FrontendLogPageVO> records) {
        Page<FrontendLogPageVO> page = new Page<FrontendLogPageVO>(pageNum, pageSize, total);
        page.setRecords(records == null ? new ArrayList<FrontendLogPageVO>() : records);
        return page;
    }

    private Set<Long> loadUserIdsByDept(String deptName) {
        if (StrUtil.isBlank(deptName)) {
            return null;
        }
        return new LinkedHashSet<Long>(loadUserIdsByDeptName(deptName.trim()));
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

    private List<Long> loadUserIdsByDeptName(String deptName) {
        if (StrUtil.isBlank(deptName)) {
            return Collections.emptyList();
        }
        List<SysDept> deptList = sysDeptService.list(new LambdaQueryWrapper<SysDept>()
                .select(SysDept::getId)
                .like(SysDept::getDeptName, deptName));
        Set<Long> deptIds = deptList.stream()
                .map(SysDept::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (deptIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<SysUser> users = sysUserService.list(new LambdaQueryWrapper<SysUser>()
                .select(SysUser::getId)
                .in(SysUser::getDeptId, deptIds));
        return users.stream()
                .map(SysUser::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    private Long resolveCurrentUserId() {
        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId == null) {
                return null;
            }
            return Long.valueOf(String.valueOf(loginId));
        } catch (Exception exception) {
            LOGGER.debug("解析前端监控上报用户失败 message={}", exception.getMessage());
            return null;
        }
    }

    private Date parseDate(String value) {
        if (StrUtil.isBlank(value)) {
            return new Date();
        }
        try {
            return DateUtil.parse(value.trim());
        } catch (Exception exception) {
            LOGGER.warn("解析前端监控时间失败 rawValue={} message={}，回退为当前时间",
                    value, exception.getMessage());
            return new Date();
        }
    }

    private String normalizeLowerText(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        return value.trim().toLowerCase();
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(xForwardedFor)) {
            String firstIp = xForwardedFor.split(",")[0].trim();
            if (StrUtil.isNotBlank(firstIp)) {
                return firstIp;
            }
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StrUtil.isNotBlank(xRealIp)) {
            return xRealIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return StrUtil.isBlank(remoteAddr) ? "unknown" : remoteAddr.trim();
    }

    private String truncate(String value, int maxLength) {
        if (StrUtil.isBlank(value)) {
            return value;
        }
        String text = value.trim();
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }
}
