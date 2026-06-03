package com.atsforge.platform;

import com.atsforge.platform.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableCaching
@EnableConfigurationProperties(AppProperties.class)
@SpringBootApplication
public class ResumePlatformApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResumePlatformApplication.class, args);
    }
}

