package com.milesight.beaveriot.blueprint.library.support;

import com.milesight.beaveriot.blueprint.library.component.BlueprintLibraryResourceResolver;
import com.milesight.beaveriot.blueprint.support.TemplateLoader;
import com.milesight.beaveriot.context.support.SpringContext;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * author: Luxb
 * create: 2025/9/9 15:09
 **/
public class DefaultTemplateLoader implements TemplateLoader {
    private final BlueprintLibraryResourceResolver blueprintLibraryResourceResolver;
    private final String blueprintPath;
    public DefaultTemplateLoader(String blueprintPath) {
        this.blueprintPath = blueprintPath;
        this.blueprintLibraryResourceResolver = SpringContext.getBean(BlueprintLibraryResourceResolver.class);
    }
    @Override
    public InputStream loadTemplate(String relativePath) {
        try {
            String resourcePath = blueprintLibraryResourceResolver.getResourcePath(blueprintPath, relativePath);
            String content = blueprintLibraryResourceResolver.getResourceContent(resourcePath);
            if (content == null) {
                throw new IllegalArgumentException("Resource content is null for path: " + resourcePath);
            }

            return new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load template: " + relativePath, e);
        }
    }
}
