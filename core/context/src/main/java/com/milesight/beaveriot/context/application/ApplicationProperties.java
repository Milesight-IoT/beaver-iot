package com.milesight.beaveriot.context.application;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * author: Luxb
 * create: 2025/9/2 14:26
 **/
@Component
@Data
@ConfigurationProperties(prefix = "application")
public class ApplicationProperties {
    private String version;
}
