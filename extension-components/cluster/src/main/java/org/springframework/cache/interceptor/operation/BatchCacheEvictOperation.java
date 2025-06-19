/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache.interceptor.operation;

import lombok.*;
import org.springframework.cache.interceptor.CacheOperation;

/**
 * Class describing a cache 'evict' operation.
 *
 * @author Costin Leau
 * @author Marcin Kamionowski
 * @since 3.1
 */
@EqualsAndHashCode(callSuper = true)
public class BatchCacheEvictOperation extends CacheOperation implements CacheKeyPrefix{

	private final boolean cacheWide;

	private final boolean beforeInvocation;

	private final String keyPrefix;


	/**
	 * Create a new {@link BatchCacheEvictOperation} instance from the given builder.
	 * @since 4.3
	 */
	public BatchCacheEvictOperation(Builder b) {
		super(b);
		this.cacheWide = b.cacheWide;
		this.beforeInvocation = b.beforeInvocation;
		this.keyPrefix = b.keyPrefix;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public boolean isCacheWide() {
		return this.cacheWide;
	}

	public boolean isBeforeInvocation() {
		return this.beforeInvocation;
	}


	/**
	 * A builder that can be used to create a {@link BatchCacheEvictOperation}.
	 * @since 4.3
	 */
	public static class Builder extends CacheOperation.Builder {

		private boolean cacheWide = false;

		private boolean beforeInvocation = false;

		private String keyPrefix;

		public void setKeyPrefix(String keyPrefix) {
			this.keyPrefix = keyPrefix;
		}

		public void setCacheWide(boolean cacheWide) {
			this.cacheWide = cacheWide;
		}

		public void setBeforeInvocation(boolean beforeInvocation) {
			this.beforeInvocation = beforeInvocation;
		}

		@Override
		protected @NonNull StringBuilder getOperationDescription() {
			StringBuilder sb = super.getOperationDescription();
			sb.append(",");
			sb.append(this.cacheWide);
			sb.append(",");
			sb.append(this.beforeInvocation);
			sb.append(",");
			sb.append(this.keyPrefix);
			return sb;
		}

		@Override
		public @NonNull BatchCacheEvictOperation build() {
			return new BatchCacheEvictOperation(this);
		}
	}

}
