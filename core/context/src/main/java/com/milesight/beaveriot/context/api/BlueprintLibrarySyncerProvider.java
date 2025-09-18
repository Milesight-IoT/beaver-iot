package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.model.BlueprintLibrary;
import java.util.function.Consumer;

/**
 * author: Luxb
 * create: 2025/9/17 9:05
 **/
public interface BlueprintLibrarySyncerProvider {
    void addListener(Consumer<BlueprintLibrary> listener);
}
