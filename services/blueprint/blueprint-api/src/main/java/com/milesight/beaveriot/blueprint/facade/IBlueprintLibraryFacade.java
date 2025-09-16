package com.milesight.beaveriot.blueprint.facade;

import com.milesight.beaveriot.blueprint.model.BlueprintLibrary;

/**
 * author: Luxb
 * create: 2025/9/15 17:13
 **/
public interface IBlueprintLibraryFacade {
    BlueprintLibrary findById(Long id);
    BlueprintLibrary getCurrentBlueprintLibrary();
}
