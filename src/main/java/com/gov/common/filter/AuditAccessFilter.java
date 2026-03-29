package com.gov.common.filter;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 职责：记录接口访问审计日志。
 * 为什么存在：保留请求方法、路径、操作者、状态码和耗时，为安全审计与运维留痕。
 * 关键输入输出：输入为请求/响应元信息，输出为 com.gov.audit 分类日志。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class AuditAccessFilter extends OncePerRequestFilter {

    private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("com.gov.audit");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        long startAt = System.currentTimeMillis();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - startAt;
            String userId = resolveUserId(request);
            AUDIT_LOGGER.info("method={}, uri={}, userId={}, status={}, durationMs={}",
                    method, uri, userId, response.getStatus(), duration);
        }
    }

    private String resolveUserId(HttpServletRequest request) {
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
