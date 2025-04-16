package com.milesight.beaveriot.resource.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * ResourceSettings class.
 *
 * @author simon
 * @date 2025/4/1
 */
@Component
@ConfigurationProperties(prefix = "resource")
@Data
public class ResourceSettings {

    private String storage;

    private Duration preSignExpire;

    private S3 s3;

    @Data
    public static class S3 {
        private String endpoint;

        private String accessKey;

        private String accessSecret;

        private String region;

        private String bucket;
    }
}
