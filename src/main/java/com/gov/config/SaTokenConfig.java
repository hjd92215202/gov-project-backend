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

    @Value("${gov.api-docs.enabled:false}")
    private boolean apiDocsEnabled;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        List<String> excludePaths = new ArrayList<String>();
        excludePaths.add("/system/login");
        excludePaths.add("/health/live");
        excludePaths.add("/health/ready");
        if (apiDocsEnabled) {
            excludePaths.add("/doc.html");
            excludePaths.add("/webjars/**");
            excludePaths.add("/swagger-resources/**");
            excludePaths.add("/v2/api-docs");
        }

        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**")
                .excludePathPatterns(excludePaths);
    }
}
