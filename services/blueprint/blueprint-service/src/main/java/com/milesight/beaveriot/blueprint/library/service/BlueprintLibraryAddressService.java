package com.milesight.beaveriot.blueprint.library.service;

import com.milesight.beaveriot.base.error.ErrorHolder;
import com.milesight.beaveriot.blueprint.library.client.response.ClientResponse;
import com.milesight.beaveriot.blueprint.library.client.utils.OkHttpUtil;
import com.milesight.beaveriot.blueprint.library.config.BlueprintLibraryConfig;
import com.milesight.beaveriot.blueprint.library.enums.BlueprintLibraryAddressErrorCode;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddressValidationResult;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryManifest;
import com.milesight.beaveriot.blueprint.library.support.YamlConverter;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/1 10:04
 **/
@Tenant
@Service
public class BlueprintLibraryAddressService {
    private final BlueprintLibraryConfig blueprintLibraryConfig;

    public BlueprintLibraryAddressService(BlueprintLibraryConfig blueprintLibraryConfig) {
        this.blueprintLibraryConfig = blueprintLibraryConfig;
    }

    public List<BlueprintLibraryAddress> getDistinctBlueprintLibraryAddresses() {
        // To be enhanced later: Retrieve blueprint library addresses from all tenants and deduplicate them.
        return List.of(blueprintLibraryConfig.getDefaultAddress());
    }

    public BlueprintLibraryAddress getCurrentBlueprintLibraryAddress() {
        // To be enhanced later: Retrieve blueprint library addresses from current tenant
        return blueprintLibraryConfig.getDefaultAddress();
    }

    public BlueprintLibraryAddressValidationResult validate(BlueprintLibraryAddress blueprintLibraryAddress) {
        if (blueprintLibraryAddress == null) {
            return BlueprintLibraryAddressValidationResult.of(List.of(ErrorHolder.of(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_NULL.getErrorCode(),
                    BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_NULL.getErrorMessage())));
        }

        List<ErrorHolder> errors = blueprintLibraryAddress.validate();
        if (!errors.isEmpty()) {
            return BlueprintLibraryAddressValidationResult.of(errors);
        }

        String manifestUrl = blueprintLibraryAddress.getRawManifestUrl();
        ClientResponse response = OkHttpUtil.get(manifestUrl);
        if (!response.isSuccessful()) {
            errors.add(ErrorHolder.of(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_NOT_REACHABLE.getErrorCode(),
                    BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_NOT_REACHABLE.getErrorMessage()));
            return BlueprintLibraryAddressValidationResult.of(errors);
        }

        String manifestContent = response.getData();
        BlueprintLibraryManifest manifest = YamlConverter.from(manifestContent, BlueprintLibraryManifest.class);
        if (manifest == null || !manifest.validate()) {
            errors.add(ErrorHolder.of(BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_INVALID.getErrorCode(),
                    BlueprintLibraryAddressErrorCode.BLUEPRINT_LIBRARY_ADDRESS_MANIFEST_INVALID.getErrorMessage()));
            return BlueprintLibraryAddressValidationResult.of(errors);
        }

        return BlueprintLibraryAddressValidationResult.of(errors, manifest);
    }
}