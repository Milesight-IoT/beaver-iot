package com.milesight.beaveriot.rule.components.code.language;

import com.milesight.beaveriot.rule.components.code.language.module.JavaScriptJsonModule;
import com.milesight.beaveriot.rule.components.code.language.module.LanguageModule;
import org.apache.camel.Predicate;
import org.apache.camel.spi.ScriptingLanguage;
import org.apache.camel.support.TypedLanguageSupport;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;

import java.util.Map;

/**
 * @author leon
 */
public class CustomizedJavaScriptLanguage extends TypedLanguageSupport implements ScriptingLanguage, LanguageWarmUp {
    @Override
    public Predicate createPredicate(String expression) {
        return new CustomizedJavaScriptExpression(expression);
    }

    @Override
    public CustomizedJavaScriptExpression createExpression(String expression) {
        return new CustomizedJavaScriptExpression(expression);
    }

    @Override
    public <T> T evaluate(String script, Map<String, Object> bindings, Class<T> resultType) {
        script = loadResource(script);
        try (Context cx = LanguageHelper.newContext(CustomizedJavaScriptExpression.LANG_ID)) {
            Value b = cx.getBindings(CustomizedJavaScriptExpression.LANG_ID);
            bindings.forEach(b::putMember);
            Value o = cx.eval(CustomizedJavaScriptExpression.LANG_ID, script);
            Object answer = o != null ? o.as(resultType) : null;
            return resultType.cast(answer);
        }
    }

    @Override
    public void warmUp() {
        try (Context cx = LanguageHelper.newContext(CustomizedJavaScriptExpression.LANG_ID)) {
            LanguageModule jsonModule = new JavaScriptJsonModule(cx);
            jsonModule.init();
        }
    }
}
