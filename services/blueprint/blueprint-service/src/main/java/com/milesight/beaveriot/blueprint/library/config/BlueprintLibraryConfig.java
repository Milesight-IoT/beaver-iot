package com.milesight.beaveriot.blueprint.library.config;

import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddressProperties;
import com.milesight.beaveriot.context.model.BlueprintLibrarySourceType;
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
    private BlueprintLibraryAddressProperties defaultAddress;

    public BlueprintLibraryAddress getDefaultBlueprintLibraryAddress() {
        return BlueprintLibraryAddress.of(defaultAddress.getType(), defaultAddress.getUrl(), defaultAddress.getBranch(), BlueprintLibrarySourceType.Default.name());
    }
}