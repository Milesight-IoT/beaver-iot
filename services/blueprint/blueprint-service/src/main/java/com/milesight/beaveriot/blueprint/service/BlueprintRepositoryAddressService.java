package com.milesight.beaveriot.blueprint.service;

import com.milesight.beaveriot.base.error.ErrorHolder;
import com.milesight.beaveriot.blueprint.client.response.ClientResponse;
import com.milesight.beaveriot.blueprint.client.utils.OkHttpUtil;
import com.milesight.beaveriot.blueprint.config.BlueprintRepositoryConfig;
import com.milesight.beaveriot.blueprint.enums.BlueprintRepositoryAddressErrorCode;
import com.milesight.beaveriot.blueprint.model.BlueprintRepositoryAddress;
import com.milesight.beaveriot.blueprint.model.BlueprintRepositoryAddressValidationResult;
import com.milesight.beaveriot.blueprint.model.BlueprintRepositoryManifest;
import com.milesight.beaveriot.blueprint.support.YamlConverter;
import com.milesight.beaveriot.permission.aspect.Tenant;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/1 10:04
 **/
@Tenant
@Service
public class BlueprintRepositoryAddressService {
    private final BlueprintRepositoryConfig blueprintRepositoryConfig;

    public BlueprintRepositoryAddressService(BlueprintRepositoryConfig blueprintRepositoryConfig) {
        this.blueprintRepositoryConfig = blueprintRepositoryConfig;
    }

    public List<BlueprintRepositoryAddress> getDistinctBlueprintRepositoryAddresses() {
        // To be enhanced later: Retrieve blueprint repository addresses from all tenants and deduplicate them.
        return List.of(blueprintRepositoryConfig.getDefaultAddress());
    }

    public BlueprintRepositoryAddress getCurrentBlueprintRepositoryAddress() {
        // To be enhanced later: Retrieve blueprint repository addresses from current tenant
        return blueprintRepositoryConfig.getDefaultAddress();
    }

    public BlueprintRepositoryAddressValidationResult validate(BlueprintRepositoryAddress blueprintRepositoryAddress) {
        if (blueprintRepositoryAddress == null) {
            return BlueprintRepositoryAddressValidationResult.of(List.of(ErrorHolder.of(BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_NULL.getErrorCode(),
                    BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_NULL.getErrorMessage())));
        }

        List<ErrorHolder> errors = blueprintRepositoryAddress.validate();
        if (!errors.isEmpty()) {
            return BlueprintRepositoryAddressValidationResult.of(errors);
        }

        String manifestUrl = blueprintRepositoryAddress.getRawManifestUrl();
        ClientResponse response = OkHttpUtil.get(manifestUrl);
        if (!response.isSuccessful()) {
            errors.add(ErrorHolder.of(BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_MANIFEST_NOT_REACHABLE.getErrorCode(),
                    BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_MANIFEST_NOT_REACHABLE.getErrorMessage()));
            return BlueprintRepositoryAddressValidationResult.of(errors);
        }

        String manifestContent = response.getData();
        BlueprintRepositoryManifest manifest = YamlConverter.from(manifestContent, BlueprintRepositoryManifest.class);
        if (manifest == null || !manifest.validate()) {
            errors.add(ErrorHolder.of(BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_MANIFEST_INVALID.getErrorCode(),
                    BlueprintRepositoryAddressErrorCode.BLUEPRINT_REPOSITORY_ADDRESS_MANIFEST_INVALID.getErrorMessage()));
            return BlueprintRepositoryAddressValidationResult.of(errors);
        }

        return BlueprintRepositoryAddressValidationResult.of(errors, manifest);
    }
}