package com.milesight.beaveriot.blueprint.library.model.support;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.blueprint.library.client.response.ClientResponse;
import com.milesight.beaveriot.blueprint.library.client.utils.OkHttpUtil;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryAddressErrorCode;
import com.milesight.beaveriot.blueprint.library.support.ZipInputStreamScanner;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;

import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * author: Luxb
 * create: 2025/10/22 15:15
 **/
public class BlueprintLibraryAddressSupport {
    private static final ResourceManagerFacade resourceManagerFacade = SpringContext.getBean(ResourceManagerFacade.class);
    public static String getManifestContentFromUrl(String manifestUrl) {
        ClientResponse response = OkHttpUtil.get(manifestUrl);
        if (response == null) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_ACCESS_FAILED).build();
        }

        if (!response.isSuccessful()) {
            throw ServiceException.with(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_ACCESS_FAILED).build();
        }

        return response.getData();
    }

    public static String getManifestContentFromResourceZip(String zipUrl, String manifestFilePath) {
        return getManifestContentFromZip(zipUrl, manifestFilePath, resourceManagerFacade::getDataByUrl);
    }

    public static String getManifestContentFromZip(String zipUrl, String manifestFilePath, Function<String, InputStream> inputStreamFetcher) {
        try (InputStream inputStream = inputStreamFetcher.apply(zipUrl)) {
            AtomicReference<String> manifestContent = new AtomicReference<>();
            boolean isSuccess = ZipInputStreamScanner.scan(inputStream, (relativePath, content) -> {
                if (relativePath.equals(manifestFilePath)) {
                    manifestContent.set(content);
                    return false;
                } else {
                    return true;
                }
            });

            if (isSuccess) {
                return manifestContent.get();
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }
}
