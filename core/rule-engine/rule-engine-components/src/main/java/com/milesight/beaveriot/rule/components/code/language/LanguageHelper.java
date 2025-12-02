package com.milesight.beaveriot.rule.components.code.language;

import com.milesight.beaveriot.rule.support.JsonHelper;
import org.apache.camel.Exchange;
import org.graalvm.polyglot.*;
import org.graalvm.polyglot.io.IOAccess;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LanguageHelper class.
 *
 * @author simon
 * @date 2025/6/9
 */
public class LanguageHelper {
    private LanguageHelper() {}

    // 1. The code cache can be controlled by keeping and maintaining strong references to the Engine and Source objects.
    // https://www.graalvm.org/latest/reference-manual/embed-languages/#code-caching-across-multiple-contexts
    // 2. The SandboxPolicy.ISOLATED and SandboxPolicy.UNTRUSTED policies are not available in GraalVM Community Edition.
    // https://www.graalvm.org/release-notes/JDK_17/
    private static final Map<String, Engine> engineCache = new ConcurrentHashMap<>();

    public static Engine getEngine(String lang) {
        return engineCache.computeIfAbsent(lang, k -> Engine
                .newBuilder(lang)
                .option("engine.WarnInterpreterOnly", "false")
                .build()
        );
    }

    public static Context newContext(String lang) {
        ResourceLimits limits = ResourceLimits.newBuilder()
                .statementLimit(2000, null)
                .build();
        return newContext(lang, limits);
    }

    public static Context newContext(String lang, ResourceLimits limits) {
        Engine engine = getEngine(lang);
        Context.Builder contextBuilder = Context.newBuilder(lang)
                .engine(engine)
                .allowHostAccess(HostAccess.NONE)
                .allowIO(IOAccess.NONE)
                .allowPolyglotAccess(PolyglotAccess.NONE)
                .allowNativeAccess(false)
                .allowHostClassLoading(false)
                .allowCreateProcess(false)
                .allowCreateThread(false)
                .allowValueSharing(false);

        if (limits != null) {
            contextBuilder.resourceLimits(limits);
        }
        return contextBuilder.build();
    }

    public static Object convertResultValue(Value value, Exchange exchange, Class<?> type) {
        if (value == null) {
            return null;
        }
        if (value.isNumber()) {
            return value.as(Number.class);
        } else if (value.isBoolean()) {
            return value.as(Boolean.class);
        } else {
            Object out = value.as(Object.class);
            if (out instanceof List<?>) {
                return JsonHelper.cast(out, List.class);
            } else if (out instanceof Map) {
                return JsonHelper.cast(out, Map.class);
            } else {
                return exchange.getContext().getTypeConverter().convertTo(type, exchange, out);
            }
        }
    }
}
