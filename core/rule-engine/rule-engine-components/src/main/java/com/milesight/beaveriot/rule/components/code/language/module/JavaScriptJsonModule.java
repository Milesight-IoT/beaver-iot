package com.milesight.beaveriot.rule.components.code.language.module;

import com.milesight.beaveriot.base.utils.JsonUtils;

/**
 * author: Luxb
 * create: 2025/11/10 17:19
 **/
public class JavaScriptJsonModule extends LanguageModule {
    public JavaScriptJsonModule() {
        super();
    }

    @Override
    protected String getLanguageId() {
        return "js";
    }

    @Override
    protected String getScriptContent() {
        return "(function(input) {" +
                "  var start = Date.now();" +
                "  while (Date.now() - start < 10000) {}" +
                "  return JSON.parse(input);" +
                "})";
    }

    @Override
    protected String getScriptName() {
        return "json_helper.js";
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