package com.milesight.beaveriot.rule.components.code.language;

import org.apache.camel.Predicate;
import org.apache.camel.spi.ScriptingLanguage;
import org.apache.camel.support.TypedLanguageSupport;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.util.Map;

import static org.graalvm.polyglot.Source.newBuilder;

/**
 * @author leon
 */
public class CustomizedPythonLanguage extends TypedLanguageSupport implements ScriptingLanguage {

    @Override
    public Predicate createPredicate(String expression) {
        return new CustomizedPythonExpression(expression);
    }

    @Override
    public CustomizedPythonExpression createExpression(String expression) {
        return new CustomizedPythonExpression(expression);
    }

    @Override
    public <T> T evaluate(String script, Map<String, Object> bindings, Class<T> resultType) {
        script = loadResource(script);
        try (Context cx = LanguageHelper.newContext(CustomizedPythonExpression.LANG_ID)) {
            Value b = cx.getBindings(CustomizedPythonExpression.LANG_ID);
            bindings.forEach(b::putMember);
            Value o = cx.eval(CustomizedPythonExpression.LANG_ID, script);
            Object answer = o != null ? o.as(resultType) : null;
            return resultType.cast(answer);
        }
    }
}
