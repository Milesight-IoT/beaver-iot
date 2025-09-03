package com.milesight.beaveriot.blueprint.support;

import javax.annotation.Nullable;
import java.io.InputStream;

@FunctionalInterface
public interface TemplateLoader {

    @Nullable
    InputStream loadTemplate(String relativePath);

}
