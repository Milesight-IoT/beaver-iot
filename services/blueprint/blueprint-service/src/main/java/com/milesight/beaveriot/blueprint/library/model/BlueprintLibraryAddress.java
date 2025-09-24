package com.milesight.beaveriot.blueprint.library.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryAddressErrorCode;
import com.milesight.beaveriot.context.model.BlueprintLibrarySourceType;
import com.milesight.beaveriot.context.model.BlueprintLibraryType;
import lombok.Data;

import java.util.Objects;


/**
 * author: Luxb
 * create: 2025/9/1 10:03
 **/
@Data
public abstract class BlueprintLibraryAddress {
    public static final String RESOURCE_TYPE = "blueprint-library-address";
    @JsonIgnore
    protected Long id;
    protected BlueprintLibraryType type;
    protected String url;
    protected String branch;
    protected BlueprintLibrarySourceType sourceType;
    protected Boolean active;
    @JsonIgnore
    protected Long createdAt;
    @JsonIgnore
    private String key;

    protected BlueprintLibraryAddress() {
    }

    public static BlueprintLibraryAddress of(String type, String url, String branch, String sourceType) {
        BlueprintLibraryType addressType = BlueprintLibraryType.of(type);
        BlueprintLibrarySourceType librarySourceType = BlueprintLibrarySourceType.of(sourceType);
        BlueprintLibraryAddress address = switch (addressType) {
            case Github -> new BlueprintLibraryGithubAddress();
            case Gitlab -> new BlueprintLibraryGitlabAddress();
            case Zip -> new BlueprintLibraryZipAddress();
        };
        address.setUrl(url);
        address.setBranch(branch);
        address.setSourceType(librarySourceType);
        address.setActive(false);
        return address;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean logicEquals(BlueprintLibraryAddress other) {
        if (other == null) {
            return false;
        }

        return type == other.type &&
                Objects.equals(url, other.getUrl()) &&
                Objects.equals(branch, other.getBranch());
    }

    public void setType(BlueprintLibraryType type) {
        this.type = type;
        this.updateKey();
    }

    public void setUrl(String url) {
        this.url = url;
        this.updateKey();
    }

    public void setBranch(String branch) {
        this.branch = branch;
        this.updateKey();
    }

    private void updateKey() {
        key = String.format("%s:%s@%s", type, url, branch);
    }

    public void validate() {
        if (StringUtils.isEmpty(url)) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_URL_EMPTY.getErrorCode(),
                    BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_URL_EMPTY.getErrorMessage()).build();
        }

        if (StringUtils.isEmpty(branch)) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_BRANCH_EMPTY.getErrorCode(),
                    BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_BRANCH_EMPTY.getErrorMessage()).build();
        }

        if (!validateUrl()) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_URL_INVALID.getErrorCode(),
                    BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_URL_INVALID.formatMessage(getUrlRegex())).build();
        }
    }

    public abstract boolean validateUrl();
    @JsonIgnore
    public abstract String getUrlRegex();
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