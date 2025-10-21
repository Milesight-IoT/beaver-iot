package com.milesight.beaveriot.blueprint.library.config;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.library.component.BlueprintLibraryAddressZipProxy;
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
    private Duration cleanFrequency;

    public BlueprintLibraryAddress getDefaultBlueprintLibraryAddress() {
        BlueprintLibraryAddress defaultBlueprintLibraryAddress = BlueprintLibraryAddress.of(defaultAddress.getType(), defaultAddress.getUrl(), defaultAddress.getBranch(), BlueprintLibrarySourceType.DEFAULT.name());
        if (!StringUtils.isEmpty(defaultAddress.getLocalZipFile())) {
            defaultBlueprintLibraryAddress.setProxy(BlueprintLibraryAddressZipProxy.of(defaultAddress.getLocalZipFile()));
        }
        return defaultBlueprintLibraryAddress;
    }
}