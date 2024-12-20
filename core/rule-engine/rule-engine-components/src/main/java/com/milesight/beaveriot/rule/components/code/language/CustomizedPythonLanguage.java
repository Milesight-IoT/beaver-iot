package com.milesight.beaveriot.rule.components.code.language;

import org.apache.camel.language.python.PythonLanguage;

/**
 * @author leon
 */
public class CustomizedPythonLanguage extends PythonLanguage {

    @Override
    public CustomizedPythonExpression createExpression(String expression) {
        return new CustomizedPythonExpression(expression, Object.class);
    }
}
