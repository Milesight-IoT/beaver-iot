package com.milesight.beaveriot.resource.adapter.db;

import com.milesight.beaveriot.resource.adapter.db.service.DbResourceService;
import com.milesight.beaveriot.resource.config.ResourceStorageType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DbResourceConfiguration class.
 *
 * @author simon
 * @date 2025/4/2
 */
@Configuration
@ConditionalOnProperty(prefix = "resource", name = "storage", havingValue = ResourceStorageType.DB, matchIfMissing = true)
public class DbResourceConfiguration {
    @Bean(name = "dbResourceAdapter")
    public DbResourceAdapter dbResourceAdapter(DbResourceService resourceService) {
        return new DbResourceAdapter(resourceService);
    }
}
