package com.milesight.beaveriot.rule.components.code.language;

import org.apache.camel.language.js.JavaScriptExpression;
import org.apache.camel.language.js.JavaScriptLanguage;

/**
 * @author leon
 */
public class CustomizedJavaScriptLanguage extends JavaScriptLanguage {
    @Override
    public JavaScriptExpression createExpression(String expression) {
        return new CustomizedJavaScriptExpression(expression, Object.class);
    }
}
