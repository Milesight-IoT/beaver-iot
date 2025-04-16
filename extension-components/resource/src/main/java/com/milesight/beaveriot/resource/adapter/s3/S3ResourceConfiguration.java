package com.milesight.beaveriot.resource.adapter.s3;

import com.milesight.beaveriot.resource.config.ResourceSettings;
import com.milesight.beaveriot.resource.config.ResourceStorageType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * S3ResourceConfiguration class.
 *
 * @author simon
 * @date 2025/4/3
 */
@Configuration
@ConditionalOnProperty(prefix = "resource", name = "storage", havingValue = ResourceStorageType.S3)
public class S3ResourceConfiguration {
    @Bean(name = "s3ResourceAdapter")
    S3ResourceAdapter s3ResourceAdapter(ResourceSettings resourceSettings) {
        return new S3ResourceAdapter(resourceSettings);
    }
}
