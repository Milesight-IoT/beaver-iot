package com.milesight.beaveriot.blueprint.library.component;

import com.milesight.beaveriot.context.api.BlueprintLibrarySyncerProvider;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

/**
 * author: Luxb
 * create: 2025/9/17 9:40
 **/
@Service
public class BlueprintLibrarySyncerProviderImpl implements BlueprintLibrarySyncerProvider {
    private final BlueprintLibrarySyncer blueprintLibrarySyncer;

    public BlueprintLibrarySyncerProviderImpl(BlueprintLibrarySyncer blueprintLibrarySyncer) {
        this.blueprintLibrarySyncer = blueprintLibrarySyncer;
    }

    @Override
    public void addListener(Consumer<BlueprintLibrary> listener) {
        blueprintLibrarySyncer.addListener(listener);
    }
}
