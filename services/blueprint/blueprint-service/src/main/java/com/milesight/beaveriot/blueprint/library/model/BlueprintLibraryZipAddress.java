package com.milesight.beaveriot.blueprint.library.model;

import com.milesight.beaveriot.context.model.BlueprintLibraryType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * author: Luxb
 * create: 2025/9/16 17:46
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class BlueprintLibraryZipAddress extends BlueprintLibraryAddress {
    public BlueprintLibraryZipAddress() {
        super();
        setType(BlueprintLibraryType.ZIP);
    }

    @Override
    public boolean validateUrl() {
        return url.matches(BlueprintLibraryAddressValidator.REGEX_ADDRESS_URL);
    }

    @Override
    public String getUrlRegex() {
        return BlueprintLibraryAddressValidator.REGEX_ADDRESS_URL;
    }

    @Override
    public String getRawManifestUrl() {
        return null;
    }

    @Override
    public String getCodeZipUrl() {
        return url;
    }

    public static class BlueprintLibraryAddressValidator {
        public static final String REGEX_ADDRESS_URL = "^(https?://|/).+\\.zip$";
    }
}
