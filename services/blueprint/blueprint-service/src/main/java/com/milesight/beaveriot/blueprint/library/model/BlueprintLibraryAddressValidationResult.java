package com.milesight.beaveriot.blueprint.library.model;

import com.milesight.beaveriot.base.error.ErrorHolder;
import lombok.Data;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/1 13:19
 **/
@Data
public class BlueprintLibraryAddressValidationResult {
    private List<ErrorHolder> errors;
    private BlueprintLibraryManifest manifest;

    private BlueprintLibraryAddressValidationResult(List<ErrorHolder> errors, BlueprintLibraryManifest manifest) {
        this.errors = errors;
        this.manifest = manifest;
    }

    public static BlueprintLibraryAddressValidationResult of(List<ErrorHolder> errors) {
        return new BlueprintLibraryAddressValidationResult(errors, null);
    }

    public static BlueprintLibraryAddressValidationResult of(List<ErrorHolder> errors, BlueprintLibraryManifest manifest) {
        return new BlueprintLibraryAddressValidationResult(errors, manifest);
    }
}
