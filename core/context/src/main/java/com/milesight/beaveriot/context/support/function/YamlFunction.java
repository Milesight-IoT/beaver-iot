package com.milesight.beaveriot.context.support.function;

import com.milesight.beaveriot.context.i18n.message.MergedResourceBundleMessageSource;
import com.milesight.beaveriot.context.support.SpringContext;

/**
 * author: Luxb
 * create: 2025/9/11 13:46
 **/
@SuppressWarnings("unused")
public class YamlFunction implements SpELTemplateFunction {
    private static final String IDENTIFIER = "fn";

    @Override
    public String getIdentifier() {
        return IDENTIFIER;
    }

    public String i18n(String messageCode, Object... args) {
        MergedResourceBundleMessageSource messageSource = SpringContext.getBean(MergedResourceBundleMessageSource.class);
        return messageSource.getMessage(messageCode, args);
    }
}
