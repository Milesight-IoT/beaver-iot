package com.milesight.beaveriot.blueprint.support;

import org.springframework.lang.Nullable;

import java.io.InputStream;

@FunctionalInterface
public interface ResourceLoader {

    @Nullable
    InputStream loadResource(String relativePath);

}
