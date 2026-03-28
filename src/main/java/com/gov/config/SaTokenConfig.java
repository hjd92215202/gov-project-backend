package com.gov.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 登录拦截配置。
 * 这个类的职责是把“哪些接口必须先登录”统一收口到 Web 层，
 * 让业务 controller 不需要重复写登录校验代码。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /**
     * 注册登录拦截器并声明放行路径。
     *
     * @param registry Spring MVC 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/**") // 拦截所有路由
                .excludePathPatterns(
                        "/system/login",     // 排除登录接口
                        "/doc.html",         // 排除 Knife4j 的入口
                        "/webjars/**",       // 排除 Swagger 静态资源
                        "/swagger-resources/**",
                        "/v2/api-docs"
                );
    }
}
