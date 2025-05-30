package org.springframework.cache.interceptor.operation;

import lombok.*;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;

/**
 * @author leon
 */
@EqualsAndHashCode(callSuper = true)
public class BatchCachePutOperation extends CacheOperation implements CacheKeyPrefix{
    /**
     * Create a new {@link org.springframework.cache.interceptor.CachePutOperation} instance from the given builder.
     *
     * @param b
     * @since 4.3
     */
    public BatchCachePutOperation(Builder b) {
        super(b);
        this.unless = b.unless;
        this.prefix = b.keyPrefix;
    }

    @Nullable
    private final String unless;

    @Nullable
    private final String prefix;

    @Nullable
    public String getUnless() {
        return this.unless;
    }

    @Nullable
    public String getKeyPrefix() {
        return prefix;
    }

    /**
     * A builder that can be used to create a {@link org.springframework.cache.interceptor.CachePutOperation}.
     * @since 4.3
     */
    public static class Builder extends CacheOperation.Builder {

        @Nullable
        private String unless;
        @Nullable
        private String keyPrefix;

        public void setUnless(@Nullable String unless) {
            this.unless = unless;
        }

        public void setKeyPrefix(@Nullable String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        @Override
        protected @NonNull StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append(" | unless='");
            sb.append(this.unless);
            sb.append("'");
            sb.append(this.keyPrefix);
            sb.append("'");
            return sb;
        }

        @Override
        public @NonNull BatchCachePutOperation build() {
            return new BatchCachePutOperation(this);
        }
    }
}
