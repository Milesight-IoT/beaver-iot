package com.milesight.beaveriot.context.i18n.locale;

import com.alibaba.ttl.TransmittableThreadLocal;
import lombok.experimental.SuperBuilder;

import java.util.Locale;
import java.util.Objects;

/**
 * author: Luxb
 * create: 2025/7/31 17:30
 **/
@SuperBuilder
public class LocaleContext {
    private static final TransmittableThreadLocal<Locale> localeThreadLocal = new TransmittableThreadLocal<>();

    /**
     * Get current Locale.
     * @return the current Locale
     */
    public static Locale getLocale() {
        Locale locale = localeThreadLocal.get();
        if (locale == null) {
            locale = Locale.ENGLISH;
        }
        return locale;
    }


    /**
     * Set the current Locale.
     * @param locale the Locale to set, must not be null
     */
    public static void setLocale(Locale locale) {
        localeThreadLocal.set(Objects.requireNonNull(locale, "Locale must not be null"));
    }
}