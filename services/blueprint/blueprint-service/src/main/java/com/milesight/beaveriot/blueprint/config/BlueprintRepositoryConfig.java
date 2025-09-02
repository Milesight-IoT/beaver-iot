package com.milesight.beaveriot.blueprint.config;

import com.milesight.beaveriot.blueprint.model.BlueprintRepositoryAddress;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * author: Luxb
 * create: 2025/9/1 9:51
 **/
@Data
@Component
@ConfigurationProperties(prefix = "blueprint.repository")
public class BlueprintRepositoryConfig {
    private Duration syncFrequency;
    private BlueprintRepositoryAddress defaultAddress;
}