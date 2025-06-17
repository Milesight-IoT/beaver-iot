package org.springframework.cache.interceptor;

import org.springframework.cache.support.SimpleValueWrapper;

/**
 * @author leon
 */
public class MissingCacheValueWrapper extends SimpleValueWrapper {

    private Object missingCacheValue;
    /**
     * Create a new SimpleValueWrapper instance for exposing the given value.
     *
     * @param value the value to expose (may be {@code null})
     */
    public MissingCacheValueWrapper(Object value, Object missingCacheValue) {
        super(value);
        this.missingCacheValue = missingCacheValue;
    }

    public Object getMissingCacheValue() {
        return missingCacheValue;
    }
}
