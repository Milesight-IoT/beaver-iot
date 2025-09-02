package com.milesight.beaveriot.blueprint.model;

import com.milesight.beaveriot.base.error.ErrorHolder;
import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/1 13:19
 **/
@Data
public class BlueprintRepositoryAddressValidationResult {
    private List<ErrorHolder> errors;
    private BlueprintRepositoryManifest manifest;

    private BlueprintRepositoryAddressValidationResult(List<ErrorHolder> errors, BlueprintRepositoryManifest manifest) {
        this.errors = errors;
        this.manifest = manifest;
    }

    public static BlueprintRepositoryAddressValidationResult of(List<ErrorHolder> errors) {
        return new BlueprintRepositoryAddressValidationResult(errors, null);
    }

    public static BlueprintRepositoryAddressValidationResult of(List<ErrorHolder> errors, BlueprintRepositoryManifest manifest) {
        return new BlueprintRepositoryAddressValidationResult(errors, manifest);
    }
}
