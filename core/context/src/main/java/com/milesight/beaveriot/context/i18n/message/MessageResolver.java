package com.milesight.beaveriot.context.i18n.message;

import com.milesight.beaveriot.context.i18n.locale.LocaleContext;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.util.StringUtils;

import java.util.Locale;

/**
 * author: Luxb
 * create: 2025/8/1 9:04
 **/
public class MessageResolver {
    private final MessageSource messageSource;

    public MessageResolver(MessageConfig config, String moduleName) {
        this.messageSource = createMessageSource(config, moduleName);
    }

    private ReloadableResourceBundleMessageSource createMessageSource(MessageConfig config, String moduleName) {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();

        String basename = config.getBasename();
        String fullBasename = injectModuleName(basename, moduleName);
        source.setBasename(fullBasename);

        source.setDefaultLocale(Locale.ROOT);

        if (config.getEncoding() != null) {
            source.setDefaultEncoding(config.getEncoding());
        }

        if (config.getCacheDuration() != null) {
            source.setCacheSeconds(config.getCacheDuration());
        }

        if (config.getFallbackToSystemLocale() != null) {
            source.setFallbackToSystemLocale(config.getFallbackToSystemLocale());
        }
        return source;
    }

    private String injectModuleName(String basename, String moduleName) {
        String fullBasename;
        if (StringUtils.hasText(moduleName)) {
            int lastSlash = basename.lastIndexOf('/');
            if (lastSlash == -1) {
                fullBasename = moduleName + "/" + basename;
            } else {
                fullBasename = basename.substring(0, lastSlash + 1) + moduleName + "/" + basename.substring(lastSlash + 1);
            }
        } else {
            fullBasename = basename;
        }
        return fullBasename;
    }

    public String message(String code, Object[] args, String defaultMessage, Locale locale) {
        try {
            if (locale == null) {
                locale = LocaleContext.getLocale();
            }
            return messageSource.getMessage(code, args, defaultMessage, locale);
        } catch (NoSuchMessageException e) {
            return defaultMessage != null ? defaultMessage : code;
        }
    }

    public String message(String code, Object[] args, String defaultMessage) {
        return message(code, args, defaultMessage, null);
    }

    public String message(String code, Object[] args, Locale locale) {
        return message(code, args, null, locale);
    }

    public String message(String code, Object[] args) {
        return message(code, args, null, null);
    }

    public String message(String code, Locale locale) {
        return message(code, null, null, locale);
    }

    public String message(String code) {
        return message(code, null, null, null);
    }
}