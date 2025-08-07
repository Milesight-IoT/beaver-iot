package com.milesight.beaveriot.context.i18n.configuration;

import com.milesight.beaveriot.context.i18n.message.MergedResourceBundleMessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.time.Duration;
import java.util.Locale;

/**
 * author: Luxb
 * create: 2025/8/6 15:31
 **/
@Configuration
public class MessageSourceAutoConfiguration {
    @Bean
    public MergedResourceBundleMessageSource messageSource(MessageSourceConfig config, PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver) {
        MergedResourceBundleMessageSource messageSource = new MergedResourceBundleMessageSource(pathMatchingResourcePatternResolver);
        String basename = config.getBasename();
        messageSource.setBasename(basename);

        messageSource.setDefaultLocale(Locale.ROOT);

        if (config.getEncoding() != null) {
            messageSource.setDefaultEncoding(config.getEncoding().name());
        }

        messageSource.setFallbackToSystemLocale(config.isFallbackToSystemLocale());

        Duration cacheDuration = config.getCacheDuration();
        if (cacheDuration != null) {
            messageSource.setCacheMillis(cacheDuration.toMillis());
        }

        messageSource.setAlwaysUseMessageFormat(config.isAlwaysUseMessageFormat());

        messageSource.setUseCodeAsDefaultMessage(config.isUseCodeAsDefaultMessage());
        return messageSource;
    }

    @Bean
    public PathMatchingResourcePatternResolver pathMatchingResourcePatternResolver() {
        return new PathMatchingResourcePatternResolver();
    }
}