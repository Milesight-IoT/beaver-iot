package com.milesight.beaveriot.blueprint.library.model;

import com.milesight.beaveriot.context.model.BlueprintLibraryType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * author: Luxb
 * create: 2025/9/16 16:10
 **/
@EqualsAndHashCode(callSuper = true)
@Data
public class BlueprintLibraryGitlabAddress extends BlueprintLibraryAddress {
    public BlueprintLibraryGitlabAddress() {
        super();
        setType(BlueprintLibraryType.Gitlab);
    }

    @Override
    public boolean validateHome() {
        return home != null && BlueprintLibraryAddressValidator.PATTERN_HOME.matcher(home).matches();
    }

    @Override
    public String getHomeRegex() {
        return BlueprintLibraryAddressValidator.REGEX_ADDRESS_HOME;
    }

    @Override
    public String getRawManifestUrl() {
        Matcher matcher = BlueprintLibraryAddressValidator.PATTERN_HOME.matcher(home);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            String repository = matcher.group(2);
            return String.format(Constants.FORMAT_MANIFEST, prefix, repository, branch);
        }
        return null;
    }

    @Override
    public String getCodeZipUrl() {
        Matcher matcher = BlueprintLibraryAddressValidator.PATTERN_HOME.matcher(home);
        if (matcher.matches()) {
            String prefix = matcher.group(1);
            String repository = matcher.group(2);
            return String.format(Constants.FORMAT_CODE_ZIP,
                    prefix, repository, branch, repository, branch);
        }
        return null;
    }

    public static class Constants {
        public static final String FORMAT_MANIFEST = "%s/%s/-/raw/%s/manifest.yaml";
        public static final String FORMAT_CODE_ZIP = "%s/%s/-/archive/%s/%s-%s.zip";
    }

    public static class BlueprintLibraryAddressValidator {
        public static final String REGEX_ADDRESS_HOME = "^(https?://[^/]+(?:/[^/]+)*)/([^/]+)\\.git$";

        public static final Pattern PATTERN_HOME = Pattern.compile(REGEX_ADDRESS_HOME);
    }
}
