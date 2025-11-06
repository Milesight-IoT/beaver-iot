package com.milesight.beaveriot.blueprint.library.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.blueprint.library.client.response.ResponseBodyInputStream;
import com.milesight.beaveriot.blueprint.library.client.utils.OkHttpUtil;
import com.milesight.beaveriot.blueprint.library.component.interfaces.BlueprintLibraryAddressProxy;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryAddressErrorCode;
import com.milesight.beaveriot.blueprint.library.model.support.BlueprintLibraryAddressSupport;
import com.milesight.beaveriot.context.model.BlueprintLibrarySourceType;
import com.milesight.beaveriot.context.model.BlueprintLibraryType;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;


/**
 * author: Luxb
 * create: 2025/9/1 10:03
 **/
@Slf4j
@Data
public abstract class BlueprintLibraryAddress {
    private static final ResourceManagerFacade resourceManagerFacade = SpringContext.getBean(ResourceManagerFacade.class);
    protected BlueprintLibraryType type;
    protected String url;
    protected String branch;
    protected BlueprintLibrarySourceType sourceType;
    protected Boolean active;
    @JsonIgnore
    private String key;
    @JsonIgnore
    private BlueprintLibraryAddressProxy proxy;
    @JsonIgnore
    private boolean proxyMode = false;

    protected BlueprintLibraryAddress() {
    }

    public static BlueprintLibraryAddress of(String type, String url, String branch, String sourceType) {
        BlueprintLibraryType addressType = BlueprintLibraryType.of(type);
        BlueprintLibrarySourceType librarySourceType = BlueprintLibrarySourceType.of(sourceType);
        BlueprintLibraryAddress address = switch (addressType) {
            case GITHUB -> new BlueprintLibraryGithubAddress();
            case GITLAB -> new BlueprintLibraryGitlabAddress();
            case ZIP -> new BlueprintLibraryZipAddress();
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

    public BlueprintLibraryAddressProxy switchAndGetProxy() {
        switchToProxy();
        return proxy;
    }

    public void switchToProxy() {
        proxyMode = true;
    }

    public static class Constants {
        public static final String PATH_MANIFEST = "manifest.yaml";
    }

    public String getManifestContent() {
        String manifestContent;
        if (proxyMode && proxy != null) {
            manifestContent = proxy.getManifestContent();
        } else {
            if (sourceType == BlueprintLibrarySourceType.UPLOAD) {
                manifestContent = BlueprintLibraryAddressSupport.getManifestContentFromResourceZip(getCodeZipUrl(), getManifestFilePath());
            } else {
                String manifestUrl = getRawManifestUrl();
                try {
                    manifestContent = BlueprintLibraryAddressSupport.getManifestContentFromUrl(manifestUrl);
                } catch (Exception e) {
                    if (proxy == null) {
                        throw e;
                    }
                    log.warn("Failed to access blueprint library {}: falling back to proxy mode", getKey());
                    manifestContent = switchAndGetProxy().getManifestContent();
                }
            }
        }
        return manifestContent;
    }

    public InputStream getDataInputStream() throws IOException {
        if (proxyMode && proxy != null) {
            return proxy.getDataInputStream();
        } else {
            if (sourceType == BlueprintLibrarySourceType.UPLOAD) {
                return resourceManagerFacade.getDataByUrl(getCodeZipUrl());
            } else {
                Request request = new Request.Builder().url(getCodeZipUrl()).build();
                Response response = null;
                try {
                    response = OkHttpUtil.getClient().newCall(request).execute();
                    return new ResponseBodyInputStream(response);
                } catch (Exception e){
                    if (response != null) {
                        response.close();
                    }
                    throw e;
                }
            }
        }
    }
}