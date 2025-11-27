package com.milesight.beaveriot.rule.components.code.language.module.pool;

import com.milesight.beaveriot.base.pool.ObjectPool;
import com.milesight.beaveriot.base.pool.component.ObjectPoolManager;
import com.milesight.beaveriot.base.pool.PoolConfig;
import com.milesight.beaveriot.rule.components.code.language.module.LanguageModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for LanguageModule pools
 * Provides convenient access to pooled LanguageModule instances
 *
 * @author Luxb
 * @date 2025/11/27
 */
@Slf4j
@Component
public class LanguageModulePoolManager {
    private final ObjectPoolManager objectPoolManager;
    private static final PoolConfig DEFAULT_CONFIG = PoolConfig.builder()
            .minIdle(2)
            .maxTotal(10)
            .maxIdleTime(Duration.ofMinutes(5))
            .evictionCheckInterval(Duration.ofMinutes(1))
            .maxWaitTime(Duration.ofSeconds(30))
            .build();

    private final Map<Class<? extends LanguageModule>, ObjectPool<? extends LanguageModule>> pools;
    private final PoolConfig config;

    public LanguageModulePoolManager(ObjectPoolManager objectPoolManager) {
        this.objectPoolManager = objectPoolManager;
        this.pools = new ConcurrentHashMap<>();
        this.config = DEFAULT_CONFIG;
    }

    /**
     * Get or create a pool for the specified LanguageModule type
     */
    @SuppressWarnings("unchecked")
    private <T extends LanguageModule> ObjectPool<T> getOrCreatePool(
            Class<T> moduleClass) {
        return (ObjectPool<T>) pools.computeIfAbsent(moduleClass, clazz -> {
            log.debug("Creating pool for {}", moduleClass.getSimpleName());
            return objectPoolManager.getPool(
                    config,
                    () -> {
                        try {
                            return moduleClass.getDeclaredConstructor().newInstance();
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
                    moduleClass
            );
        });
    }

    public <T extends LanguageModule, R> R executeWithResult(
            Class<T> moduleClass,
            ModuleWithResultOperation<T, R> operation) throws Exception {
        ObjectPool<T> pool = getOrCreatePool(moduleClass);
        return pool.execute(operation::execute);
    }

    public <T extends LanguageModule> void executeWithoutResult(
            Class<T> moduleClass,
            ModuleWithoutResultOperation<T> operation) throws Exception {
        ObjectPool<T> pool = getOrCreatePool(moduleClass);
        pool.execute(operation::execute);
    }

    public void initModule(Class<? extends LanguageModule> moduleClass) {
        try {
            executeWithoutResult(moduleClass, LanguageModule::init);
        } catch (Exception e) {
            log.error("Failed to init module", e);
        }
    }

    public Object executeModule(Class<? extends LanguageModule> moduleClass, Object input) {
        try {
            return executeWithResult(moduleClass, module -> module.input(input));
        } catch (Exception e) {
            log.error("Failed to execute module", e);
            return input;
        }
    }

    @FunctionalInterface
    public interface ModuleWithResultOperation<T extends LanguageModule, R> {
        R execute(T module) throws Exception;
    }

    @FunctionalInterface
    public interface ModuleWithoutResultOperation<T extends LanguageModule> {
        void execute(T module) throws Exception;
    }
}
