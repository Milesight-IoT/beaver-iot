package com.milesight.beaveriot.blueprint.core.helper;

import com.milesight.beaveriot.blueprint.support.ResourceLoader;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

@RequiredArgsConstructor
public class BlueprintResourceBundleControl extends ResourceBundle.Control {

    private final ResourceLoader resourceLoader;

    @Override
    public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader,
                                    boolean reload)
            throws IOException {

        var bundleName = toBundleName(baseName, locale);
        var resourceName = toResourceName(bundleName, "properties");

        try (var stream = resourceLoader.loadResource(resourceName)) {
            if (stream != null) {
                return new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }
        }
        return null;
    }
}
