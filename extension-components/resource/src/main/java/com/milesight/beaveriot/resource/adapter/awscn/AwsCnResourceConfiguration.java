package com.milesight.beaveriot.resource.adapter.awscn;

import com.milesight.beaveriot.resource.config.ResourceSettings;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AwsCnResourceConfiguration class.
 *
 * @author simon
 * @date 2025/4/3
 */
@Configuration
@ConditionalOnProperty(prefix = "resource", name = "storage", havingValue = "aws-cn")
public class AwsCnResourceConfiguration {
    @Bean(name = "awsResourceAdapter")
    public AwsCnResourceAdapter awsResourceAdapter(ResourceSettings resourceSettings) {
        return new AwsCnResourceAdapter(resourceSettings);
    }
}
