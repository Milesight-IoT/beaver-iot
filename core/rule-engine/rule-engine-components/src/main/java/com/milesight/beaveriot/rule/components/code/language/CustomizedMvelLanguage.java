package com.milesight.beaveriot.rule.components.code.language;

import org.apache.camel.Predicate;
import org.apache.camel.language.mvel.MvelLanguage;

/**
 * @author leon
 */
public class CustomizedMvelLanguage extends MvelLanguage {

    @Override
    public Predicate createPredicate(String expression) {
        return new CustomizedMvelExpression(loadResource(expression), Boolean.class);
    }

    @Override
    public CustomizedMvelExpression createExpression(String expression) {
        return new CustomizedMvelExpression(expression, Object.class);
    }
}
