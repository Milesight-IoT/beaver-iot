package com.milesight.beaveriot.blueprint.model;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * author: Luxb
 * create: 2025/9/1 11:40
 **/
@Data
public class BlueprintRepositoryManifest {
    private String version;
    private String minimumRequiredBeaverIoTVersion;
    private String author;
    private String deviceVendorIndex;
    private String solutionIndex;

    public boolean validate() {
        return StringUtils.hasText(version) &&
                StringUtils.hasText(minimumRequiredBeaverIoTVersion) &&
                StringUtils.hasText(author) &&
                StringUtils.hasText(deviceVendorIndex) &&
                StringUtils.hasText(solutionIndex);
    }
}