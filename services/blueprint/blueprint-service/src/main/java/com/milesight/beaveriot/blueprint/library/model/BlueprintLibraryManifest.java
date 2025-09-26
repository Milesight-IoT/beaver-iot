package com.milesight.beaveriot.blueprint.library.model;

import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * author: Luxb
 * create: 2025/9/1 11:40
 **/
@Data
public class BlueprintLibraryManifest {
    private String version;
    private String minimumRequiredBeaverIotVersion;
    private String author;
    private String deviceVendorIndex;
    private String solutionIndex;

    public boolean validate() {
        return StringUtils.hasText(version) &&
                StringUtils.hasText(minimumRequiredBeaverIotVersion) &&
                StringUtils.hasText(author) &&
                StringUtils.hasText(deviceVendorIndex) &&
                StringUtils.hasText(solutionIndex);
    }
}