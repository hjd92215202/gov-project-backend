package com.gov.common.filter;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.gov.common.security.ClientIpResolver;
import com.gov.module.system.entity.SysAuditLog;
import com.gov.module.system.service.SysAuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * 记录接口访问审计信息并输出慢请求观测日志。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuditAccessFilter extends OncePerRequestFilter {

    private static final String AUDIT_USER_ID_ATTR = "audit.userId";
    private static final String TRACE_ID_KEY = "traceId";
    private static final Logger LOGGER = LoggerFactory.getLogger(AuditAccessFilter.class);
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("com.gov.audit");
    private static final Logger PERF_LOGGER = LoggerFactory.getLogger("com.gov.perf");

    @Autowired(required = false)
    private SysAuditLogService sysAuditLogService;

    @Autowired(required = false)
    private ClientIpResolver clientIpResolver;

    @Value("${gov.logging.slow-request-ms:800}")
    private long slowRequestThresholdMs;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request == null ? null : request.getRequestURI();
        return StrUtil.isNotBlank(uri) && uri.contains("/system/frontend-monitor/report");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startAt = System.currentTimeMillis();
        Date requestTime = new Date(startAt);
        String method = request.getMethod();
        String uri = request.getRequestURI();

        String resolvedUserId = resolveUserId(request);
        if (StrUtil.isNotBlank(resolvedUserId) && !"anonymous".equals(resolvedUserId)) {
            request.setAttribute(AUDIT_USER_ID_ATTR, resolvedUserId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = System.currentTimeMillis() - startAt;
            String userId = resolveUserId(request);
            String clientIp = resolveClientIp(request);
            String traceId = MDC.get(TRACE_ID_KEY);
            int status = response.getStatus();

            AUDIT_LOGGER.info("method={}, uri={}, userId={}, ip={}, status={}, durationMs={}",
                    method, uri, userId, clientIp, status, durationMs);
            if (durationMs >= slowRequestThresholdMs) {
                LOGGER.warn("慢请求告警 method={} uri={} userId={} status={} durationMs={} traceId={}",
                        method, uri, userId, status, durationMs, traceId);
            }

            long enqueueStartAt = System.currentTimeMillis();
            boolean enqueued = enqueueAuditLog(userId, method, uri, clientIp, status, durationMs, traceId, requestTime, request);
            long enqueueMs = System.currentTimeMillis() - enqueueStartAt;

            PERF_LOGGER.info("audit_enqueue_perf method={} uri={} userId={} enqueued={} enqueueMs={} totalDurationMs={} traceId={}",
                    method, uri, userId, enqueued, enqueueMs, durationMs, traceId);
        }
    }

    private boolean enqueueAuditLog(String userId, String method, String uri, String clientIp, int status,
                                    long durationMs, String traceId, Date requestTime, HttpServletRequest request) {
        if (sysAuditLogService == null) {
            return false;
        }
        try {
            SysAuditLog auditLog = new SysAuditLog();
            auditLog.setUserId(parseLong(userId));
            auditLog.setRequestMethod(method);
            auditLog.setRequestUri(uri);
            auditLog.setClientIp(clientIp);
            auditLog.setUserAgent(truncate(request.getHeader("User-Agent"), 500));
            auditLog.setHttpStatus(status);
            auditLog.setDurationMs(durationMs);
            auditLog.setTraceId(truncate(traceId, 64));
            auditLog.setRequestTime(requestTime);
            sysAuditLogService.saveAsync(auditLog);
            return true;
        } catch (Exception exception) {
            LOGGER.warn("审计日志入队失败 uri={} userId={} traceId={} message={}",
                    uri, userId, traceId, exception.getMessage());
            return false;
        }
    }

    private String resolveUserId(HttpServletRequest request) {
        Object attrUserId = request.getAttribute(AUDIT_USER_ID_ATTR);
        if (attrUserId != null && StrUtil.isNotBlank(String.valueOf(attrUserId))) {
            return String.valueOf(attrUserId);
        }

        try {
            Object loginId = StpUtil.getLoginIdDefaultNull();
            if (loginId != null) {
                return String.valueOf(loginId);
            }
        } catch (Exception exception) {
            LOGGER.debug("读取当前登录上下文失败，继续尝试 Cookie 反查 uri={} message={}",
                    request == null ? null : request.getRequestURI(), exception.getMessage());
        }

        String tokenValue = extractTokenValue(request);
        if (StrUtil.isNotBlank(tokenValue)) {
            try {
                String normalizedTokenValue = tokenValue.startsWith("Bearer ")
                        ? tokenValue.substring("Bearer ".length()).trim()
                        : tokenValue;
                Object loginIdByToken = StpUtil.getLoginIdByToken(normalizedTokenValue);
                if (loginIdByToken != null) {
                    return String.valueOf(loginIdByToken);
                }
            } catch (Exception exception) {
                LOGGER.debug("根据 token 反查登录用户失败 uri={} tokenPrefix={} message={}",
                        request == null ? null : request.getRequestURI(),
                        tokenValue.length() > 8 ? tokenValue.substring(0, 8) : tokenValue,
                        exception.getMessage());
            }
        }

        return "anonymous";
    }

    private Long parseLong(String value) {
        if (StrUtil.isBlank(value) || "anonymous".equals(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException exception) {
            LOGGER.debug("审计 userId 解析失败 value={} message={}", value, exception.getMessage());
            return null;
        }
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

    private String resolveClientIp(HttpServletRequest request) {
        if (clientIpResolver != null) {
            return clientIpResolver.resolveClientIp(request);
        }
        if (request == null) {
            return "unknown";
        }
        String forwardedFor = StrUtil.trimToNull(request.getHeader("X-Forwarded-For"));
        if (StrUtil.isNotBlank(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = StrUtil.trimToNull(request.getHeader("X-Real-IP"));
        if (StrUtil.isNotBlank(realIp)) {
            return realIp;
        }
        return StrUtil.blankToDefault(StrUtil.trimToEmpty(request.getRemoteAddr()), "unknown");
    }

    private String extractTokenValue(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String tokenName = "Authorization";
        try {
            String configuredTokenName = StpUtil.getTokenName();
            if (StrUtil.isNotBlank(configuredTokenName)) {
                tokenName = configuredTokenName.trim();
            }
        } catch (Exception exception) {
            LOGGER.debug("读取 tokenName 配置失败，回退到默认 token 名 uri={} message={}",
                    request == null ? null : request.getRequestURI(), exception.getMessage());
        }

        String headerToken = StrUtil.trimToNull(request.getHeader(tokenName));
        if (headerToken == null) {
            headerToken = StrUtil.trimToNull(request.getHeader("Authorization"));
        }
        if (headerToken != null) {
            return headerToken;
        }

        if (request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookie != null && tokenName.equals(cookie.getName())) {
                return StrUtil.trimToNull(cookie.getValue());
            }
        }
        return null;
    }
}
