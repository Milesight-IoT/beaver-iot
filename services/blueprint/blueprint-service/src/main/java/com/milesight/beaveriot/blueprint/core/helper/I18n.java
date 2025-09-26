package com.milesight.beaveriot.blueprint.core.helper;

import com.milesight.beaveriot.blueprint.support.ResourceLoader;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.LocaleUtils;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;

@RequiredArgsConstructor
public class I18n {

    public static final String DEFAULT_BASE_NAME = "locale/messages";

    private final ResourceLoader resourceLoader;

    private final Locale locale;

    private Map<String, ResourceBundle> bundleCache;

    public String t(String key) {
        return t(DEFAULT_BASE_NAME, key, null);
    }

    public String t(String key, List<String> params) {
        return t(DEFAULT_BASE_NAME, key, params);
    }

    public String t(String baseName, String key) {
        return t(baseName, key, null);
    }

    public String t(String baseName, String key, List<String> params) {
        return t(null, baseName, key, params);
    }

    public String t(String localeString, String baseName, String key, List<String> params) {
        if (baseName == null || key == null) {
            return null;
        }

        var currentLocale = localeString != null
                ? LocaleUtils.toLocale(localeString)
                : locale;

        if (bundleCache == null) {
            bundleCache = new ConcurrentHashMap<>(3);
        }

        var bundle = bundleCache.computeIfAbsent(currentLocale.toLanguageTag(), k ->
                ResourceBundle.getBundle(baseName, currentLocale, new BlueprintResourceBundleControl(resourceLoader)));

        var str = bundle.getString(key);
        if (params == null) {
            return str;
        }
        return MessageFormat.format(str, params.toArray());
    }

}
