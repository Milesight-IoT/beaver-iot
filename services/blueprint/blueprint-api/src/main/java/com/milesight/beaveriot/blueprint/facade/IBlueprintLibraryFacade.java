package com.milesight.beaveriot.blueprint.facade;

import com.milesight.beaveriot.context.model.BlueprintLibrary;

/**
 * author: Luxb
 * create: 2025/9/15 17:13
 **/
public interface IBlueprintLibraryFacade {
    BlueprintLibrary findById(Long id);
    void deleteById(Long id);
    BlueprintLibrary getCurrentBlueprintLibrary();
}
