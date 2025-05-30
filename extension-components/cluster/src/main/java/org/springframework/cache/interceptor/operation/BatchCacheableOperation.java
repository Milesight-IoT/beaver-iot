package org.springframework.cache.interceptor.operation;

import lombok.EqualsAndHashCode;
import org.springframework.cache.interceptor.CacheOperation;
import org.springframework.lang.Nullable;

/**
 * @author leon
 */
@EqualsAndHashCode
public class BatchCacheableOperation extends CacheOperation {

	@Nullable
	private final String unless;

	private final boolean sync;


	/**
	 * Create a new {@link BatchCacheableOperation} instance from the given builder.
	 * @since 4.3
	 */
	public BatchCacheableOperation(BatchCacheableOperation.Builder b) {
		super(b);
		this.unless = b.unless;
		this.sync = b.sync;
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

		public void setUnless(String unless) {
			this.unless = unless;
		}

		public void setSync(boolean sync) {
			this.sync = sync;
		}

		@Override
		protected StringBuilder getOperationDescription() {
			StringBuilder sb = super.getOperationDescription();
			sb.append(" | unless='");
			sb.append(this.unless);
			sb.append('\'');
			sb.append(" | sync='");
			sb.append(this.sync);
			sb.append('\'');
			return sb;
		}

		@Override
		public BatchCacheableOperation build() {
			return new BatchCacheableOperation(this);
		}
	}

}