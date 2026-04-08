package com.gov.common.security;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * 统一管理双提交 CSRF Cookie。
 */
@Component
public class CsrfTokenManager {

    @Value("${gov.security.csrf.cookie-name:XSRF-TOKEN}")
    private String cookieName;

    @Value("${gov.security.csrf.header-name:X-CSRF-Token}")
    private String headerName;

    @Value("${sa-token.cookie.path:/}")
    private String cookiePath;

    @Value("${sa-token.cookie.same-site:Lax}")
    private String sameSite;

    @Value("${sa-token.cookie.secure:false}")
    private boolean secure;

    public String ensureToken(HttpServletRequest request, HttpServletResponse response) {
        String existingToken = readTokenFromCookie(request);
        if (StrUtil.isNotBlank(existingToken)) {
            return existingToken;
        }
        return issueToken(response);
    }

    public String issueToken(HttpServletResponse response) {
        String token = UUID.randomUUID().toString().replace("-", "");
        writeCookie(response, token, -1);
        return token;
    }

    public void clearToken(HttpServletResponse response) {
        writeCookie(response, "", 0);
    }

    public String readTokenFromCookie(HttpServletRequest request) {
        if (request == null || request.getCookies() == null) {
            return null;
        }
        for (Cookie cookie : request.getCookies()) {
            if (cookie != null && cookieName.equals(cookie.getName())) {
                return StrUtil.trimToNull(cookie.getValue());
            }
        }
        return null;
    }

    public String getHeaderName() {
        return headerName;
    }

    private void writeCookie(HttpServletResponse response, String value, long maxAgeSeconds) {
        if (response == null) {
            return;
        }
        ResponseCookie cookie = ResponseCookie.from(cookieName, StrUtil.nullToEmpty(value))
                .path(StrUtil.blankToDefault(cookiePath, "/"))
                .httpOnly(false)
                .secure(secure)
                .sameSite(StrUtil.blankToDefault(sameSite, "Lax"))
                .maxAge(maxAgeSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
