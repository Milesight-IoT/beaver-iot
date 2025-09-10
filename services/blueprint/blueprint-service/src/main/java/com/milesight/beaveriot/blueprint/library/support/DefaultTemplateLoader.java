package com.milesight.beaveriot.blueprint.library.support;

import com.milesight.beaveriot.context.api.BlueprintLibraryResourceProvider;
import com.milesight.beaveriot.context.support.TemplateLoader;
import com.milesight.beaveriot.context.support.SpringContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * author: Luxb
 * create: 2025/9/9 15:09
 **/
public class DefaultTemplateLoader implements TemplateLoader {
    private final BlueprintLibraryResourceProvider blueprintLibraryResourceProvider;
    private final String blueprintPath;
    public DefaultTemplateLoader(String blueprintPath) {
        this.blueprintPath = blueprintPath;
        this.blueprintLibraryResourceProvider = SpringContext.getBean(BlueprintLibraryResourceProvider.class);
    }
    @Override
    public InputStream loadTemplate(String relativePath) {
        try {
            String resourcePath = blueprintLibraryResourceProvider.getResourcePath(blueprintPath, relativePath);
            String content = blueprintLibraryResourceProvider.getResourceContent(resourcePath);
            if (content == null) {
                throw new IllegalArgumentException("Resource content is null for path: " + resourcePath);
            }

            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template: " + relativePath, e);
        }
    }
}
