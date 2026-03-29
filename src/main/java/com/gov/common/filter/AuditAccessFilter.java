package com.gov.common.filter;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.gov.module.system.entity.SysAuditLog;
import com.gov.module.system.service.SysAuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;

/**
 * 职责：记录接口访问审计日志，并落库用于后台审计页面查询。
 * 为什么存在：统一沉淀“谁在何时从哪个 IP 调用了什么接口、结果如何”。
 * 关键输入输出：输入为请求/响应元信息，输出为审计日志文件 + `sys_audit_log` 表记录。
 * 关联链路：全站接口 -> AuditAccessFilter -> 审计日志与审计列表。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuditAccessFilter extends OncePerRequestFilter {

    private static final String AUDIT_USER_ID_ATTR = "audit.userId";
    private static final String TRACE_ID_KEY = "traceId";
    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("com.gov.audit");

    @Autowired(required = false)
    private SysAuditLogService sysAuditLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startAt = System.currentTimeMillis();
        Date requestTime = new Date(startAt);
        String method = request.getMethod();
        String uri = request.getRequestURI();

        // 在进入业务链路前先尝试一次，避免请求结束后上下文清理导致丢失登录态信息。
        String earlyResolvedUserId = resolveUserId(request);
        if (StrUtil.isNotBlank(earlyResolvedUserId) && !"anonymous".equals(earlyResolvedUserId)) {
            request.setAttribute(AUDIT_USER_ID_ATTR, earlyResolvedUserId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startAt;
            String userId = resolveUserId(request);
            String clientIp = resolveClientIp(request);
            String traceId = MDC.get(TRACE_ID_KEY);
            int status = response.getStatus();

            AUDIT_LOGGER.info("method={}, uri={}, userId={}, ip={}, status={}, durationMs={}",
                    method, uri, userId, clientIp, status, duration);
            persistAuditLog(userId, method, uri, clientIp, status, duration, traceId, requestTime, request);
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
        } catch (Exception ignored) {
            // ignore
        }

        String tokenValue = extractTokenValue(request);
        if (StrUtil.isNotBlank(tokenValue)) {
            try {
                Object loginIdByToken = StpUtil.getLoginIdByToken(tokenValue);
                if (loginIdByToken != null) {
                    return String.valueOf(loginIdByToken);
                }
            } catch (Exception ignored) {
                // ignore
            }
        }

        return "anonymous";
    }

    private void persistAuditLog(String userId, String method, String uri, String clientIp, int status,
                                 long durationMs, String traceId, Date requestTime, HttpServletRequest request) {
        if (sysAuditLogService == null) {
            return;
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
            sysAuditLogService.save(auditLog);
        } catch (Exception ignored) {
            // 审计落库失败不影响主业务请求链路。
        }
    }

    private Long parseLong(String value) {
        if (StrUtil.isBlank(value) || "anonymous".equals(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(xForwardedFor)) {
            String firstIp = xForwardedFor.split(",")[0].trim();
            if (StrUtil.isNotBlank(firstIp)) {
                return firstIp;
            }
        }
        String[] headerCandidates = {
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
        };
        for (String header : headerCandidates) {
            String value = request.getHeader(header);
            if (StrUtil.isNotBlank(value) && !"unknown".equalsIgnoreCase(value.trim())) {
                return value.trim();
            }
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

    private String extractTokenValue(HttpServletRequest request) {
        String tokenName = "Authorization";
        try {
            String configuredTokenName = StpUtil.getTokenName();
            if (StrUtil.isNotBlank(configuredTokenName)) {
                tokenName = configuredTokenName.trim();
            }
        } catch (Exception ignored) {
            // ignore
        }

        String tokenValue = request.getHeader(tokenName);
        if (StrUtil.isBlank(tokenValue)) {
            tokenValue = request.getHeader("Authorization");
        }
        if (StrUtil.isBlank(tokenValue)) {
            tokenValue = request.getParameter(tokenName);
        }
        if (StrUtil.isBlank(tokenValue)) {
            tokenValue = request.getParameter("Authorization");
        }
        if (StrUtil.isBlank(tokenValue)) {
            return null;
        }

        tokenValue = tokenValue.trim();
        if (tokenValue.startsWith("Bearer ")) {
            tokenValue = tokenValue.substring("Bearer ".length()).trim();
        }
        return tokenValue;
    }
}
