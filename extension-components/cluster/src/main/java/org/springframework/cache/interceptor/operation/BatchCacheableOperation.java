package org.springframework.cache.interceptor.operation;

import lombok.*;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;

/**
 * @author leon
 */
@EqualsAndHashCode(callSuper = true)
public class BatchCacheableOperation extends CacheOperation implements CacheKeyPrefix {

	@Nullable
	private final String unless;

	private final boolean sync;

	private final String keyPrefix;
	/**
	 * Create a new {@link BatchCacheableOperation} instance from the given builder.
	 * @since 4.3
	 */
	public BatchCacheableOperation(BatchCacheableOperation.Builder b) {
		super(b);
		this.unless = b.unless;
		this.sync = b.sync;
		this.keyPrefix = b.keyPrefix;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	@Nullable
	public String getUnless() {
		return this.unless;
	}

	public boolean isSync() {
		return this.sync;
	}


	/**
	 * A builder that can be used to create a {@link BatchCacheableOperation}.
	 * @since 4.3
	 */
	public static class Builder extends CacheOperation.Builder {

		@Nullable
		private String unless;

		private boolean sync;

		private String keyPrefix;

		public void setUnless(@Nullable String unless) {
			this.unless = unless;
		}

		public void setKeyPrefix(String keyPrefix) {
			this.keyPrefix = keyPrefix;
		}

		public void setSync(boolean sync) {
			this.sync = sync;
		}

		@Override
		protected @NonNull StringBuilder getOperationDescription() {
			StringBuilder sb = super.getOperationDescription();
			sb.append(" | unless='");
			sb.append(this.unless);
			sb.append('\'');
			sb.append(" | sync='");
			sb.append(this.sync);
			sb.append('\'');
			sb.append(',');
			sb.append(this.keyPrefix);
			return sb;
		}

		@Override
		public @NonNull BatchCacheableOperation build() {
			return new BatchCacheableOperation(this);
		}
	}

}
