package com.leadproject.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AppProperties.class,
        AiProperties.class,
        VoiceScriptProperties.class,
        CorsProperties.class,
        SecurityProperties.class
})
public class AppConfig {
}