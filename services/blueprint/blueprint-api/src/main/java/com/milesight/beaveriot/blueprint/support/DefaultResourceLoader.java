package com.milesight.beaveriot.blueprint.support;

import com.milesight.beaveriot.blueprint.facade.IBlueprintLibraryResourceResolverFacade;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.support.SpringContext;
import org.springframework.lang.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * author: Luxb
 * create: 2025/9/9 15:09
 **/
public class DefaultResourceLoader implements ResourceLoader {
    private final IBlueprintLibraryResourceResolverFacade blueprintLibraryResourceFacade;
    private final BlueprintLibrary blueprintLibrary;
    private final String blueprintPath;
    public DefaultResourceLoader(BlueprintLibrary blueprintLibrary, String blueprintPath) {
        this.blueprintLibrary = blueprintLibrary;
        this.blueprintPath = blueprintPath;
        this.blueprintLibraryResourceFacade = SpringContext.getBean(IBlueprintLibraryResourceResolverFacade.class);
    }

    @Nullable
    @Override
    public InputStream loadResource(String relativePath) {
        try {
            String resourcePath = blueprintLibraryResourceFacade.buildResourcePath(blueprintPath, relativePath);
            String content = blueprintLibraryResourceFacade.getResourceContent(blueprintLibrary, resourcePath);
            if (content == null) {
                return null;
            }

            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load resource: " + relativePath, e);
        }
    }
}
