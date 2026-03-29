package com.gov;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 后端应用启动入口。
 * 这个类本身不承载业务逻辑，存在的意义是统一装配 Spring Boot 自动配置，
 * 并作为整个政务项目后端的根启动点。
 */
@SpringBootApplication
@EnableScheduling
public class GovApplication {

    /**
     * 启动 Spring Boot 应用。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(GovApplication.class, args);
    }
}
