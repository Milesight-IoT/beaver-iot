package com.milesight.beaveriot.base.pool;

/**
 * Factory for creating pooled objects
 *
 * @param <T> the type of objects created by this factory
 * @author Luxb
 * @date 2025/11/27
 */
@FunctionalInterface
public interface ObjectFactory<T> {
    /**
     * Create a new object instance
     *
     * @return newly created object
     * @throws Exception if object creation fails
     */
    T create() throws Exception;
}
