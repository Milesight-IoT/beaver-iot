package com.milesight.beaveriot.resource.adapter.aws;

import com.milesight.beaveriot.resource.config.ResourceSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AwsResourceConfiguration class.
 *
 * @author simon
 * @date 2025/4/2
 */
@Configuration
@ConditionalOnProperty(prefix = "resource", name = "storage", havingValue = "aws")
public class AwsResourceConfiguration {
    @Bean(name = "awsResourceAdapter")
    public AwsResourceAdapter awsResourceAdapter(ResourceSettings resourceSettings) {
        return new AwsResourceAdapter(resourceSettings);
    }
}
