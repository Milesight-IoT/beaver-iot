package net.javacrumbs.shedlock.spring.aop;

import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import lombok.Getter;
import net.javacrumbs.shedlock.core.ClockProvider;
import net.javacrumbs.shedlock.core.LockConfiguration;

import java.time.Duration;

/**
 * @author leon
 */
@Getter
public class ScopedLockConfiguration extends LockConfiguration {

    private LockScope lockScope;

    private boolean throwOnLockFailure = false;

    private Duration waitForLock;
    protected ScopedLockConfiguration(LockConfiguration lockConfiguration, LockScope lockScope, boolean throwOnLockFailure, Duration waitForLock) {
        super(ClockProvider.now(), lockConfiguration.getName(), lockConfiguration.getLockAtMostFor(), lockConfiguration.getLockAtLeastFor());
        this.lockScope = lockScope;
        this.throwOnLockFailure = throwOnLockFailure;
        this.waitForLock = waitForLock;
    }

    public static ScopedLockConfigurationBuilder builder(LockScope scope) {
        return new ScopedLockConfigurationBuilder(scope);
    }

    public static class ScopedLockConfigurationBuilder {
        private final LockScope lockScope;
        private String name;
        private Duration lockAtMostFor;
        private Duration lockAtLeastFor;
        private boolean throwOnLockFailure = true;
        private Duration waitForLock;
        ScopedLockConfigurationBuilder(LockScope lockScope) {
            this.lockScope = lockScope;
        }
        public ScopedLockConfigurationBuilder throwOnLockFailure(boolean throwOnLockFailure) {
            this.throwOnLockFailure = throwOnLockFailure;
            return this;
        }
        public ScopedLockConfigurationBuilder name(String name) {
            this.name = name;
            return this;
        }
        public ScopedLockConfigurationBuilder waitForLock(Duration waitForLock) {
            this.waitForLock = waitForLock;
            return this;
        }
        public ScopedLockConfigurationBuilder lockAtMostFor(Duration lockAtMostFor) {
            this.lockAtMostFor = lockAtMostFor;
            return this;
        }
        public ScopedLockConfigurationBuilder lockAtLeastFor(Duration lockAtLeastFor) {
            this.lockAtLeastFor = lockAtLeastFor;
            return this;
        }
        public ScopedLockConfiguration build() {
            LockConfiguration lockConfiguration = new LockConfiguration(ClockProvider.now(), wrapLockNameWithNamespace(name, lockScope), lockAtMostFor, lockAtLeastFor);
            return new ScopedLockConfiguration(lockConfiguration, lockScope, throwOnLockFailure, waitForLock);
        }

        private String wrapLockNameWithNamespace(String name, LockScope lockScope) {
            switch (lockScope) {
                case TENANT:
                    return TenantContext.getTenantId() + ":" + name;
                default:
                    return name;
            }
        }
    }
}
