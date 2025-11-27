package com.milesight.beaveriot.base.pool;

/**
 * Destructor for destroying pooled objects
 *
 * @param <T> the type of objects destroyed by this destructor
 * @author Luxb
 * @date 2025/11/27
 */
@FunctionalInterface
public interface ObjectDestructor<T> {
    /**
     * Destroy an object and release its resources
     *
     * @param object the object to destroy
     * @throws Exception if destruction fails
     */
    void destroy(T object) throws Exception;

    /**
     * No-op destructor that does nothing
     */
    static <T> ObjectDestructor<T> noOp() {
        return obj -> {
            // Do nothing
        };
    }
}