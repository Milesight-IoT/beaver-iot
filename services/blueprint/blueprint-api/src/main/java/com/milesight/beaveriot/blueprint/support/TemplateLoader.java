package com.milesight.beaveriot.blueprint.support;

import java.io.InputStream;

/**
 * author: Luxb
 * create: 2025/9/9 14:16
 **/
@FunctionalInterface
public interface TemplateLoader {
    InputStream loadTemplate(String relativePath);
}