package net.javacrumbs.shedlock.core;

import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.aop.ScopedLockConfiguration;

import java.time.Duration;
import java.util.Optional;

/**
 * @author leon
 */
@Slf4j
public class RetryableLockProvider implements LockProvider {

    private final LockProvider lockProvider;

    private final long retryInterval = 150L;

    public RetryableLockProvider(LockProvider lockProvider) {
        this.lockProvider = lockProvider;
    }

    public Optional<SimpleLock> lock(LockConfiguration lockConfiguration) {

        Duration waitForLock = lockConfiguration instanceof ScopedLockConfiguration scopedLockConfiguration ?
                scopedLockConfiguration.getWaitForLock() : null;

        if (waitForLock == null || waitForLock.toMillis() == 0) {
            return lockProvider.lock(lockConfiguration);
        }

        long startTime = System.currentTimeMillis();
        long remainingTime = waitForLock.toMillis();

        try {
            while (remainingTime > 0) {
                Optional<SimpleLock> lock = lockProvider.lock(lockConfiguration);
                if (lock.isPresent()) {
                    return lock;
                }

                long elapsed = System.currentTimeMillis() - startTime;
                remainingTime = waitForLock.toMillis() - elapsed;

                if (remainingTime > 0) {
                    log.info(">>Retrying to acquire lock: " + lockConfiguration.getName());
                    Thread.sleep(Math.min(retryInterval, remainingTime));
                }
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch(Exception e){
            log.error("acquire lock error:{}", e.getMessage(), e);
        }
        return Optional.empty();
    }
}
