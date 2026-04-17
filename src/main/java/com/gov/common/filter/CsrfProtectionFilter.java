package com.gov.common.filter;

import cn.dev33.satoken.exception.NotWebContextException;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.StrUtil;
import com.gov.common.security.CsrfTokenManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
 * 对已登录用户的写操作启用双提交 Cookie CSRF 校验。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class CsrfProtectionFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CsrfProtectionFilter.class);
    private static final String API_CONTEXT_PREFIX = "/api";

    @Autowired
    private CsrfTokenManager csrfTokenManager;

    @Value("${gov.security.csrf.enabled:true}")
    private boolean enabled;

    @Value("${gov.api-docs.enabled:false}")
    private boolean apiDocsEnabled;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || request == null) {
            return true;
        }
        String method = StrUtil.blankToDefault(request.getMethod(), "GET").toUpperCase();
        if ("GET".equals(method) || "HEAD".equals(method) || "OPTIONS".equals(method)) {
            return true;
        }
        String uri = StrUtil.blankToDefault(request.getRequestURI(), "");
        if (isWhitelistedUri(uri, "/system/login")
                || isWhitelistedUri(uri, "/health/live")
                || isWhitelistedUri(uri, "/health/ready")) {
            return true;
        }
        return apiDocsEnabled && (isWhitelistedUri(uri, "/doc.html")
                || isWhitelistedUri(uri, "/webjars/")
                || isWhitelistedUri(uri, "/swagger-resources/")
                || isWhitelistedUri(uri, "/v2/api-docs"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (!isLoginSafely(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String cookieToken = csrfTokenManager.readTokenFromCookie(request);
        String headerToken = StrUtil.trimToNull(request.getHeader(csrfTokenManager.getHeaderName()));
        if (StrUtil.isBlank(cookieToken) || !StrUtil.equals(cookieToken, headerToken)) {
            LOGGER.warn("CSRF validation failed uri={} method={} userId={} hasCookie={} hasHeader={}",
                    request.getRequestURI(), request.getMethod(), StpUtil.getLoginIdDefaultNull(),
                    StrUtil.isNotBlank(cookieToken), StrUtil.isNotBlank(headerToken));
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"非法请求，请刷新页面后重试\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isLoginSafely(HttpServletRequest request) {
        try {
            return StpUtil.isLogin();
        } catch (NotWebContextException exception) {
            LOGGER.debug("Skip CSRF check because Sa-Token web context is unavailable uri={} message={}",
                    request == null ? null : request.getRequestURI(), exception.getMessage());
            return false;
        } catch (RuntimeException exception) {
            LOGGER.warn("Skip CSRF check due to unexpected login state failure uri={} message={}",
                    request == null ? null : request.getRequestURI(), exception.getMessage());
            return false;
        }
    }

    private boolean isWhitelistedUri(String requestUri, String rawPath) {
        String uri = StrUtil.blankToDefault(requestUri, "");
        String path = StrUtil.blankToDefault(rawPath, "");
        if (StrUtil.isBlank(uri) || StrUtil.isBlank(path)) {
            return false;
        }
        return uri.startsWith(path) || uri.startsWith(API_CONTEXT_PREFIX + path);
    }
}
