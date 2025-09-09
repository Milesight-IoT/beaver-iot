package com.milesight.beaveriot.blueprint.library.config;

import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
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
@ConfigurationProperties(prefix = "blueprint.library")
public class BlueprintLibraryConfig {
    private Duration syncFrequency;
    private BlueprintLibraryAddress defaultAddress;
}