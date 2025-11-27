package com.milesight.beaveriot.rule.components.code.language.module.pool;

import com.milesight.beaveriot.base.pool.ObjectPool;
import com.milesight.beaveriot.base.pool.PoolConfig;
import com.milesight.beaveriot.base.pool.component.ObjectPoolManager;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.rule.components.code.language.module.JavaScriptJsonModule;
import com.milesight.beaveriot.rule.components.code.language.module.LanguageModule;
import com.milesight.beaveriot.rule.components.code.language.module.PythonJsonModule;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * @author Luxb
 * @date 2025/11/27 15:56
 **/
@Slf4j
public class LanguageModulePool<T extends LanguageModule> {
    private static final PoolConfig DEFAULT_CONFIG = PoolConfig.builder()
            .minIdle(2)
            .maxTotal(10)
            .maxIdleTime(Duration.ofMinutes(5))
            .evictionCheckInterval(Duration.ofMinutes(1))
            .maxWaitTime(Duration.ofSeconds(30))
            .build();
    private static final ObjectPoolManager objectPoolManager = SpringContext.getBean(ObjectPoolManager.class);
    private final ObjectPool<T> pool;

    private LanguageModulePool(Class<T> moduleClass) {
        pool = objectPoolManager.getPool(
                DEFAULT_CONFIG,
                () -> {
                    try {
                        T module = moduleClass.getDeclaredConstructor().newInstance();
                        module.init();
                        return module;
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to create instance of " + moduleClass, e);
                    }
                },
                module -> {
                    try {
                        module.close();
                    } catch (Exception e) {
                        log.error("Failed to close module", e);
                    }
                },
                moduleClass);
    }

    public Object execute(Object input) {
        try {
            return pool.execute(module -> module.input(input));
        } catch (Exception e) {
            log.error("Failed to execute module", e);
            return input;
        }
    }

    private static class Instance {
        static final LanguageModulePool<JavaScriptJsonModule> INSTANCE_JAVASCRIPT_JSON = new LanguageModulePool<>(JavaScriptJsonModule.class);
        static final LanguageModulePool<PythonJsonModule> INSTANCE_PYTHON_JSON = new LanguageModulePool<>(PythonJsonModule.class);
    }

    public static LanguageModulePool<JavaScriptJsonModule> getJavaScriptJsonModulePool() {
        return Instance.INSTANCE_JAVASCRIPT_JSON;
    }

    public static LanguageModulePool<PythonJsonModule> getPythonJsonModulePool() {
        return Instance.INSTANCE_PYTHON_JSON;
    }
}
