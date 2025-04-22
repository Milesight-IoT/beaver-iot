package com.milesight.beaveriot.base.annotations.shedlock;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@SchedulerLock
public @interface DistributedLock {

    @AliasFor(annotation = SchedulerLock.class)
    String name() default "";

    /**
     * How long the lock should be kept in case the machine which obtained the lock
     * died before releasing it. This is just a fallback, under normal circumstances
     * the lock is released as soon the tasks finishes.
     *
     * <p>
     * Can be either time with suffix like 10s or ISO8601 duration as described in
     * {@link java.time.Duration#parse(CharSequence)}, for example PT30S.
     */
    @AliasFor(annotation = SchedulerLock.class)
    String lockAtMostFor() default "";

    /**
     * The lock will be held at least for given duration. Can be used if you really
     * need to execute the task at most once in given period of time. If the
     * duration of the task is shorter than clock difference between nodes, the task
     * can be theoretically executed more than once (one node after another). By
     * setting this parameter, you can make sure that the lock will be kept at least
     * for given period of time.
     *
     * <p>
     * Can be either time with suffix like 10s or ISO8601 duration as described in
     * {@link java.time.Duration#parse(CharSequence)}, for example PT30S.
     */
    @AliasFor(annotation = SchedulerLock.class)
    String lockAtLeastFor() default "";

    /**
     * Scope, when tenant isolation is enabled, the scope of the lock is the tenant (when the tenant context exists)
     * @return
     */
    LockScope scope() default LockScope.TENANT;

    /**
     * If true, the lock will throw an exception if it cannot be obtained. If false, the lock will be silently ignored.
     * @return
     */
    boolean throwOnLockFailure() default true;

    /**
     * The wait time for the lock
     * @return
     */
    String waitForLock() default "";
}
