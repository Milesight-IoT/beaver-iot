package com.milesight.beaveriot.rule.components.code.language.module;

import com.milesight.beaveriot.base.utils.JsonUtils;
import org.graalvm.polyglot.Context;

/**
 * author: Luxb
 * create: 2025/11/10 17:19
 **/
public class PythonJsonModule extends LanguageModule {
    public PythonJsonModule(Context cx) {
        super(cx);
    }

    @Override
    protected String getLanguageId() {
        return "python";
    }

    @Override
    protected String getScriptContent() {
        return """
                import json
                json.loads
                """;
    }

    @Override
    protected String getScriptName() {
        return "json_helper.py";
    }

    @Override
    protected boolean isSkipSimpleValue() {
        return true;
    }

    @Override
    protected Object transformInput(Object obj) {
        return JsonUtils.withDefaultStrategy().toJSON(obj);
    }
}