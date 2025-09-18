package com.milesight.beaveriot.blueprint.library.model;

import com.milesight.beaveriot.context.model.BlueprintLibraryType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.regex.Pattern;

/**
 * author: Luxb
 * create: 2025/9/16 17:46
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class BlueprintLibraryZipAddress extends BlueprintLibraryAddress {
    public BlueprintLibraryZipAddress() {
        super();
        setType(BlueprintLibraryType.Zip);
    }

    @Override
    public boolean validateHome() {
        return home.matches(BlueprintLibraryAddressValidator.REGEX_ADDRESS_HOME);
    }

    @Override
    public String getHomeRegex() {
        return BlueprintLibraryAddressValidator.REGEX_ADDRESS_HOME;
    }

    @Override
    public String getRawManifestUrl() {
        return null;
    }

    @Override
    public String getCodeZipUrl() {
        return home;
    }

    public static class BlueprintLibraryAddressValidator {
        public static final String REGEX_ADDRESS_HOME = "^https?://.+\\.zip$";
        public static final Pattern PATTERN_HOME = Pattern.compile(REGEX_ADDRESS_HOME);
    }
}
