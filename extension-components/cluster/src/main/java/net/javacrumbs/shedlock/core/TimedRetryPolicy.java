package net.javacrumbs.shedlock.core;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

@Slf4j
public abstract class TimedRetryPolicy {
    private final Duration waitTime;
    private final long retryInterval = 150L;

    public TimedRetryPolicy(Duration waitTime) {
        this.waitTime = waitTime;
    }

    public LockingTaskExecutor.TaskResult<Object> retry() throws Throwable {
        if (waitTime == null || waitTime.toMillis() == 0) {
            return doRetry();
        }

        long startTime = System.currentTimeMillis();
        long remainingTime = waitTime.toMillis();

        try {
            while (remainingTime > 0) {
                LockingTaskExecutor.TaskResult<Object> objectTaskResult = doRetry();
                if (objectTaskResult.wasExecuted()) {
                    return objectTaskResult;
                }

                long elapsed = System.currentTimeMillis() - startTime;
                remainingTime = waitTime.toMillis() - elapsed;

                if (remainingTime > 0) {
                    Thread.sleep(Math.min(retryInterval, remainingTime));
                }
            }
        } catch(InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch(Exception e){
            log.error("acquire lock error:{}", e.getMessage(), e);
        }
        return LockingTaskExecutor.TaskResult.notExecuted();
    }

    protected abstract LockingTaskExecutor.TaskResult<Object> doRetry() throws Throwable;

}