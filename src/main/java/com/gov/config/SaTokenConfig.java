package com.gov.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token 登录拦截配置。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private static final String API_CONTEXT_PREFIX = "/api";

    @Value("${gov.api-docs.enabled:false}")
    private boolean apiDocsEnabled;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePaths = new ArrayList<String>();
        addPathVariants(excludePaths, "/system/login");
        addPathVariants(excludePaths, "/system/frontend-monitor/report");
        addPathVariants(excludePaths, "/health/live");
        addPathVariants(excludePaths, "/health/ready");
        if (apiDocsEnabled) {
            addPathVariants(excludePaths, "/doc.html");
            addPathVariants(excludePaths, "/webjars/**");
            addPathVariants(excludePaths, "/swagger-resources/**");
            addPathVariants(excludePaths, "/v2/api-docs");
        }

        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(excludePaths);
    }

    private void addPathVariants(List<String> target, String rawPath) {
        if (target == null || rawPath == null) {
            return;
        }
        String normalized = rawPath.trim();
        if (normalized.isEmpty()) {
            return;
        }
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }

        addPathIfAbsent(target, normalized);
        if (!normalized.startsWith(API_CONTEXT_PREFIX + "/")) {
            addPathIfAbsent(target, API_CONTEXT_PREFIX + normalized);
        }
    }

    private void addPathIfAbsent(List<String> target, String path) {
        if (!target.contains(path)) {
            target.add(path);
        }
    }
}
