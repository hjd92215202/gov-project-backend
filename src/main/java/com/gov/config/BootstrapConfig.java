package com.gov.config;

import com.gov.config.properties.BootstrapProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(BootstrapProperties.class)
public class BootstrapConfig {
}
