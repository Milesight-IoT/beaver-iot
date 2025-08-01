package com.milesight.beaveriot.context.i18n.message;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * author: Luxb
 * create: 2025/8/1 10:55
 **/
@Data
@Component
@ConfigurationProperties(prefix = "spring.messages")
public class MessageConfig {
    private String basename;
    private String encoding = "UTF-8";
    private Integer cacheDuration = 3600;
    private Boolean fallbackToSystemLocale = true;
}
