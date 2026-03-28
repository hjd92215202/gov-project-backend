package com.gov.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Long 序列化配置。
 * 统一把 Long 输出成字符串，避免前端 JavaScript 在处理超长整数时发生精度丢失。
 */
@Configuration
public class JacksonLongToStringConfig {

    /**
     * 为包装类型和基本类型 Long 都注册字符串序列化器。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> builder.serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }
}
