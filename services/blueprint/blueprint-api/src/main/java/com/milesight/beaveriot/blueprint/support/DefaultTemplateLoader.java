package com.milesight.beaveriot.blueprint.support;

import com.milesight.beaveriot.blueprint.facade.IBlueprintLibraryResourceResolverFacade;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.support.SpringContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * author: Luxb
 * create: 2025/9/9 15:09
 **/
public class DefaultTemplateLoader implements TemplateLoader {
    private final IBlueprintLibraryResourceResolverFacade blueprintLibraryResourceFacade;
    private final BlueprintLibrary blueprintLibrary;
    private final String blueprintPath;
    public DefaultTemplateLoader(BlueprintLibrary blueprintLibrary, String blueprintPath) {
        this.blueprintLibrary = blueprintLibrary;
        this.blueprintPath = blueprintPath;
        this.blueprintLibraryResourceFacade = SpringContext.getBean(IBlueprintLibraryResourceResolverFacade.class);
    }
    @Override
    public InputStream loadTemplate(String relativePath) {
        try {
            String resourcePath = blueprintLibraryResourceFacade.buildResourcePath(blueprintPath, relativePath);
            String content = blueprintLibraryResourceFacade.getResourceContent(blueprintLibrary, resourcePath);
            if (content == null) {
                throw new IllegalArgumentException("Resource content is null for path: " + resourcePath);
            }

            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template: " + relativePath, e);
        }
    }
}
