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
public class BlueprintLibraryGithubAddress extends BlueprintLibraryAddress {
    public BlueprintLibraryGithubAddress() {
        super();
        setType(BlueprintLibraryType.GITHUB);
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
        Matcher matcher = BlueprintLibraryAddressValidator.PATTERN_URL.matcher(url);
        if (matcher.matches()) {
            String username = matcher.group(1);
            String repository = matcher.group(2);
            return String.format(Constants.FORMAT_MANIFEST,
                    username, repository, branch);
        } else {
            return null;
        }
    }

    @Override
    public String getCodeZipUrl() {
        Matcher matcher = BlueprintLibraryAddressValidator.PATTERN_URL.matcher(url);
        if (matcher.matches()) {
            String username = matcher.group(1);
            String repository = matcher.group(2);
            return String.format(Constants.FORMAT_CODE_ZIP,
                    username, repository, branch);
        } else {
            return null;
        }
    }

    public static class Constants {
        public static final String FORMAT_MANIFEST = "https://raw.githubusercontent.com/%s/%s/refs/heads/%s/manifest.yaml";
        public static final String FORMAT_CODE_ZIP = "https://github.com/%s/%s/archive/refs/heads/%s.zip";
    }

    public static class BlueprintLibraryAddressValidator {
        public static final String REGEX_ADDRESS_URL = "^https://github\\.com/([a-zA-Z\\d](?:[a-zA-Z\\d]|-(?=[a-zA-Z\\d])){0,38})/([a-zA-Z\\d](?:[a-zA-Z\\d._-]*[a-zA-Z\\d])?)\\.git$";
        public static final Pattern PATTERN_URL = Pattern.compile(REGEX_ADDRESS_URL);
    }
}
