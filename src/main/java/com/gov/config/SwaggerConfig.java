package com.gov.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;

/**
 * Swagger 文档配置。
 * 这个类存在的意义是把后端 controller 自动暴露成可浏览的接口文档，
 * 方便前后端联调、测试和接口巡检。
 */
@Configuration
public class SwaggerConfig {

    /**
     * 注册 Swagger Docket。
     * 这里统一指定扫描 `com.gov.module` 下的接口，避免把无关 Bean 暴露到文档中。
     *
     * @return Swagger Docket 配置对象
     */
    @Bean
    public Docket defaultApi2() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 指定要扫描的接口包路径 (后续我们的接口都写在 controller 下)
                .apis(RequestHandlerSelectors.basePackage("com.gov.module"))
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 构造接口文档基础信息。
     *
     * @return 文档元信息
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("信创政务系统 - 接口文档")
                .description("提供给前端 Vue3 的 RESTful API 文档")
                .contact(new Contact("hjd", "", ""))
                .version("1.0.0")
                .build();
    }
}
