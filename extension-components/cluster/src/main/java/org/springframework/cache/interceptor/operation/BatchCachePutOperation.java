package org.springframework.cache.interceptor.operation;

import lombok.EqualsAndHashCode;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;

/**
 * @author leon
 */
@EqualsAndHashCode
public class BatchCachePutOperation extends CacheOperation {
    /**
     * Create a new {@link org.springframework.cache.interceptor.CachePutOperation} instance from the given builder.
     *
     * @param b
     * @since 4.3
     */
    public BatchCachePutOperation(Builder b) {
        super(b);
        this.unless = b.unless;
    }

    @Nullable
    private final String unless;


    @Nullable
    public String getUnless() {
        return this.unless;
    }


    /**
     * A builder that can be used to create a {@link org.springframework.cache.interceptor.CachePutOperation}.
     * @since 4.3
     */
    public static class Builder extends CacheOperation.Builder {

        @Nullable
        private String unless;

        public void setUnless(String unless) {
            this.unless = unless;
        }

        @Override
        protected StringBuilder getOperationDescription() {
            StringBuilder sb = super.getOperationDescription();
            sb.append(" | unless='");
            sb.append(this.unless);
            sb.append("'");
            return sb;
        }

        @Override
        public BatchCachePutOperation build() {
            return new BatchCachePutOperation(this);
        }
    }
}