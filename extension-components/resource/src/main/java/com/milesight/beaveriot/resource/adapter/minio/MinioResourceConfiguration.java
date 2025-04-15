package com.milesight.beaveriot.resource.adapter.minio;

import com.milesight.beaveriot.resource.config.ResourceSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinioResourceConfiguration class.
 *
 * @author simon
 * @date 2025/4/3
 */
@Configuration
@ConditionalOnProperty(prefix = "resource", name = "storage", havingValue = "minio")
public class MinioResourceConfiguration {
    @Bean(name = "minioResourceAdapter")
    MinioResourceAdapter minioResourceAdapter(ResourceSettings resourceSettings) {
        return new MinioResourceAdapter(resourceSettings);
    }
}
