package com.milesight.beaveriot.context.i18n.message;

import com.milesight.beaveriot.context.i18n.config.MessageConfig;
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

    public MessageResolver(MessageConfig config, String integrationId, String moduleName) {
        this.messageSource = createMessageSource(config, integrationId, moduleName);
    }

    private ReloadableResourceBundleMessageSource createMessageSource(MessageConfig config, String integrationId, String moduleName) {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();

        String basename = config.getBasename();
        String fullBasename = injectBasename(basename, integrationId, moduleName);
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

    private String injectBasename(String basename, String integrationId, String moduleName) {
        if (!StringUtils.hasText(moduleName) && !StringUtils.hasText(integrationId)) {
            return basename;
        }

        int firstSlash = basename.indexOf('/');
        String prefix = (firstSlash == -1) ? "" : basename.substring(0, firstSlash + 1);
        String remainder = (firstSlash == -1) ? basename : basename.substring(firstSlash + 1);

        StringBuilder resultPath = new StringBuilder();

        if (StringUtils.hasText(prefix)) {
            resultPath.append(prefix);
        }

        if (StringUtils.hasText(integrationId)) {
            resultPath.append("integrations/").append(integrationId).append("/");
        }

        if (StringUtils.hasText(moduleName)) {
            resultPath.append(moduleName).append("/");
        }

        resultPath.append(remainder);

        return resultPath.toString();
    }

    /**
     * Retrieves a localized message based on the message code, arguments, default message, and locale.
     * If the message for the given code is not found, the default message is returned.
     * If the default message is null, the message code itself is returned.
     *
     * @param code the message code, must not be null
     * @param args an array of arguments for message placeholders, may be null
     * @param defaultMessage the default message to return if no message is found, may be null
     * @param locale the locale to use for message resolution; if null, the current thread's locale is used
     * @return the resolved message string
     */
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

    /**
     * Retrieves a localized message using the message code, arguments, and default message,
     * using the current thread's locale.
     *
     * @param code the message code, must not be null
     * @param args an array of arguments for message placeholders, may be null
     * @param defaultMessage the default message to return if no message is found, may be null
     * @return the resolved message string
     * @see #message(String, Object[], String, Locale)
     */
    public String message(String code, Object[] args, String defaultMessage) {
        return message(code, args, defaultMessage, null);
    }

    /**
     * Retrieves a localized message using the message code, arguments, and locale,
     * with no default message specified.
     *
     * @param code the message code, must not be null
     * @param args an array of arguments for message placeholders, may be null
     * @param locale the locale to use for message resolution; if null, the current thread's locale is used
     * @return the resolved message string (or the code if no message is found and no default is provided)
     * @see #message(String, Object[], String, Locale)
     */
    public String message(String code, Object[] args, Locale locale) {
        return message(code, args, null, locale);
    }

    /**
     * Retrieves a localized message using the message code and arguments,
     * with no default message and using the current thread's locale.
     *
     * @param code the message code, must not be null
     * @param args an array of arguments for message placeholders, may be null
     * @return the resolved message string (or the code if no message is found)
     * @see #message(String, Object[], String, Locale)
     */
    public String message(String code, Object[] args) {
        return message(code, args, null, null);
    }

    /**
     * Retrieves a localized message using the message code and locale,
     * with no message arguments and no default message.
     *
     * @param code the message code, must not be null
     * @param locale the locale to use for message resolution; if null, the current thread's locale is used
     * @return the resolved message string (or the code if no message is found)
     * @see #message(String, Object[], String, Locale)
     */
    public String message(String code, Locale locale) {
        return message(code, null, null, locale);
    }

    /**
     * Retrieves a localized message using only the message code,
     * with no arguments, no default message, and using the current thread's locale.
     *
     * @param code the message code, must not be null
     * @return the resolved message string (or the code if no message is found)
     * @see #message(String, Object[], String, Locale)
     */
    public String message(String code) {
        return message(code, null, null, null);
    }
}