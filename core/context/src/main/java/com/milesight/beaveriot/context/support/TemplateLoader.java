package com.milesight.beaveriot.context.support;

import java.io.InputStream;

/**
 * author: Luxb
 * create: 2025/9/9 14:16
 **/
@FunctionalInterface
public interface TemplateLoader {
    InputStream loadTemplate(String relativePath);
}