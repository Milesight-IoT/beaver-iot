package com.milesight.beaveriot.blueprint.library.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryAddressErrorCode;
import com.milesight.beaveriot.context.model.BlueprintLibraryType;
import lombok.Data;

import java.util.Objects;


/**
 * author: Luxb
 * create: 2025/9/1 10:03
 **/
@Data
public abstract class BlueprintLibraryAddress {
    @JsonIgnore
    protected Long id;
    protected BlueprintLibraryType type;
    protected String home;
    protected String branch;
    protected Boolean active;
    @JsonIgnore
    protected Long createdAt;
    @JsonIgnore
    private String key;

    protected BlueprintLibraryAddress() {
    }

    public static BlueprintLibraryAddress of(String type, String home, String branch) {
        BlueprintLibraryType addressType = BlueprintLibraryType.of(type);
        BlueprintLibraryAddress address = switch (addressType) {
            case Github -> new BlueprintLibraryGithubAddress();
            case Gitlab -> new BlueprintLibraryGitlabAddress();
            case Zip -> new BlueprintLibraryZipAddress();
        };
        address.setHome(home);
        address.setBranch(branch);
        address.setActive(false);
        return address;
    }

    public boolean logicEquals(BlueprintLibraryAddress other) {
        if (other == null) {
            return false;
        }

        return type == other.type &&
                Objects.equals(home, other.getHome()) &&
                Objects.equals(branch, other.getBranch());
    }

    public void setType(BlueprintLibraryType type) {
        this.type = type;
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
        key = String.format("%s:%s@%s", type, home, branch);
    }

    public void validate() {
        if (StringUtils.isEmpty(home)) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_HOME_EMPTY.getErrorCode(),
                    BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_HOME_EMPTY.getErrorMessage()).build();
        }

        if (StringUtils.isEmpty(branch)) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_BRANCH_EMPTY.getErrorCode(),
                    BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_BRANCH_EMPTY.getErrorMessage()).build();
        }

        if (!validateHome()) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_HOME_INVALID.getErrorCode(),
                    BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_HOME_INVALID.formatMessage(getHomeRegex())).build();
        }
    }

    public abstract boolean validateHome();
    @JsonIgnore
    public abstract String getHomeRegex();
    @JsonIgnore
    public abstract String getRawManifestUrl();
    @JsonIgnore
    public abstract String getCodeZipUrl();
    @JsonIgnore
    public String getManifestFilePath() {
        return Constants.PATH_MANIFEST;
    }

    private static class Constants {
        public static final String PATH_MANIFEST = "manifest.yaml";
    }
}