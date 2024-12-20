package com.milesight.beaveriot.rule.components.code.language;

import org.apache.camel.language.mvel.MvelLanguage;

/**
 * @author leon
 */
public class CustomizedMvelLanguage extends MvelLanguage {

    @Override
    public CustomizedMvelExpression createExpression(String expression) {
        return new CustomizedMvelExpression(expression, Object.class);
    }
}
