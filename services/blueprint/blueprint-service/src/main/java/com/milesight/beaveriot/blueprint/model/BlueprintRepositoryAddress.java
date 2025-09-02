package com.milesight.beaveriot.blueprint.model;

import com.milesight.beaveriot.base.error.ErrorHolder;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.enums.BlueprintRepositoryAddressErrorCode;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * author: Luxb
 * create: 2025/9/1 10:03
 **/
@Data
public class BlueprintRepositoryAddress {
    private String home;
    private String branch;
    private String key;

    public BlueprintRepositoryAddress() {
    }

    public BlueprintRepositoryAddress(String home, String branch) {
        this.home = home;
        this.branch = branch;
        this.updateKey();
    }

    public void setHome(String home) {
        this.home = home;
        this.updateKey();
    }

    public void setBranch(String branch) {
        this.branch = branch;
        this.updateKey();
    }

    private void updateKey() {
        key = String.format("%s@%s", home, branch);
    }

    public List<ErrorHolder> validate() {
        List<ErrorHolder> errors = new ArrayList<>();
        if (StringUtils.isEmpty(home)) {
            errors.add(ErrorHolder.of(BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_HOME_EMPTY.getErrorCode(),
                    BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_HOME_EMPTY.getErrorMessage()));
            return errors;
        }

        if (StringUtils.isEmpty(branch)) {
            errors.add(ErrorHolder.of(BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_BRANCH_EMPTY.getErrorCode(),
                    BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_BRANCH_EMPTY.getErrorMessage()));
            return errors;
        }

        if (!BlueprintRepositoryAddressValidator.validateHome(home)) {
            errors.add(ErrorHolder.of(BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_HOME_INVALID.getErrorCode(),
                    BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_HOME_INVALID.formatMessage(BlueprintRepositoryAddressValidator.REGEX_ADDRESS_HOME),
                    Map.of(ExtraDataConstants.KEY_REGEX, BlueprintRepositoryAddressValidator.REGEX_ADDRESS_HOME)));
        }

        if (!BlueprintRepositoryAddressValidator.validateBranch(branch)) {
            errors.add(ErrorHolder.of(BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_BRANCH_INVALID.getErrorCode(),
                    BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_BRANCH_INVALID.formatMessage(BlueprintRepositoryAddressValidator.REGEX_ADDRESS_BRANCH),
                    Map.of(ExtraDataConstants.KEY_REGEX, BlueprintRepositoryAddressValidator.REGEX_ADDRESS_BRANCH)));
        }

        return errors;
    }

    public String getRawManifestUrl() {
        Matcher matcher = BlueprintRepositoryAddressValidator.PATTERN_HOME.matcher(home);
        if (matcher.matches()) {
            String username = matcher.group(1);
            String repository = matcher.group(2);
            return String.format(Constants.FORMAT_MANIFEST,
                    username, repository, branch);
        } else {
            return null;
        }
    }

    public String getCodeZipUrl() {
        Matcher matcher = BlueprintRepositoryAddressValidator.PATTERN_HOME.matcher(home);
        if (matcher.matches()) {
            String username = matcher.group(1);
            String repository = matcher.group(2);
            return String.format(Constants.FORMAT_CODE_ZIP,
                    username, repository, branch);
        } else {
            return null;
        }
    }

    private static class ExtraDataConstants {
        public static final String KEY_REGEX = "regex";
    }

    public static class Constants {
        public static final String FORMAT_MANIFEST = "https://raw.githubusercontent.com/%s/%s/%s/manifest.yaml";
        public static final String FORMAT_CODE_ZIP = "https://github.com/%s/%s/archive/%s.zip";
    }

    public static class BlueprintRepositoryAddressValidator {
        public static final String REGEX_ADDRESS_HOME = "^https://github\\.com/([a-zA-Z\\d](?:[a-zA-Z\\d]|-(?=[a-zA-Z\\d])){0,38})/([a-zA-Z\\d](?:[a-zA-Z\\d._-]*[a-zA-Z\\d])?)\\.git$";
        public static final Pattern PATTERN_HOME = Pattern.compile(REGEX_ADDRESS_HOME);
        public static final String REGEX_ADDRESS_BRANCH = "^(?!/)(?!.*/$)(?!.*\\.\\.)(?!.*\\.lock$)(?!.*[ ~^:?*\\[\\\\%])[a-zA-Z0-9][a-zA-Z0-9._-]*(?:/[a-zA-Z0-9._-]+)*$";

        public static boolean validateHome(String home) {
            return home.matches(REGEX_ADDRESS_HOME);
        }

        public static boolean validateBranch(String branch) {
            return branch.matches(REGEX_ADDRESS_BRANCH);
        }
    }

    public static void main(String[] args) {
        System.out.println(BlueprintRepositoryAddressValidator.validateHome("https://github.com/Luxb/Milesight-Blueprint-Repository.git"));
        System.out.println(BlueprintRepositoryAddressValidator.validateHome("https://github.com/-Luxb/a"));
    }
}