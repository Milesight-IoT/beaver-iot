package com.milesight.beaveriot.rule.components.code.language.module;

import com.milesight.beaveriot.rule.components.code.language.LanguageHelper;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import java.io.Closeable;
import java.io.IOException;

/**
 * author: Luxb
 * create: 2025/11/11 9:29
 **/
public abstract class LanguageModule implements Closeable {
    private final Context cx;
    private final Source scriptSource;
    private volatile Value module;

    protected LanguageModule() {
        cx = LanguageHelper.newContext(getLanguageId(), null);
        try {
            scriptSource = Source.newBuilder(getLanguageId(),
                    getScriptContent(),
                    getScriptName()
            ).build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract String getLanguageId();
    protected abstract String getScriptContent();
    protected abstract String getScriptName();
    protected abstract boolean isSkipSimpleValue();
    protected abstract Object transformInput(Object obj);

    public void init() {
        if (module == null) {
            synchronized (this) {
                // Double check
                if (module == null) {
                    module = cx.eval(scriptSource);
                }
            }
        }
    }

    public Object input(Object obj) {
        if (obj == null) {
            return null;
        }

        if (isSkipSimpleValue() && isSimpleValue(obj)) {
            return obj;
        }

        init();

        obj = transformInput(obj);
        return module.execute(obj);
    }

    private static boolean isSimpleValue(Object obj) {
        return obj instanceof String || obj instanceof Number || obj instanceof Boolean;
    }

    @Override
    public void close() {
        cx.close();
        module = null;
    }

    private static class Instance {
        static final LanguageModule INSTANCE_JAVASCRIPT_JSON = new JavaScriptJsonModule();
        static final LanguageModule INSTANCE_PYTHON_JSON = new PythonJsonModule();
    }

    public static LanguageModule getJavaScriptJsonModule() {
        return Instance.INSTANCE_JAVASCRIPT_JSON;
    }

    public static LanguageModule getPythonJsonModule() {
        return Instance.INSTANCE_PYTHON_JSON;
    }
}