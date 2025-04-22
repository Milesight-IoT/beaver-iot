package net.javacrumbs.shedlock.spring.aop;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.spring.ExtendedLockConfigurationExtractor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Optional;

/**
 * @author leon
 */
public class ScopedSpringLockConfigurationExtractor implements ExtendedLockConfigurationExtractor {

    private final StringToDurationConverter durationConverter = StringToDurationConverter.INSTANCE;
    private SpringLockConfigurationExtractor springLockConfigurationExtractor;

    public ScopedSpringLockConfigurationExtractor(SpringLockConfigurationExtractor springLockConfigurationExtractor) {
        this.springLockConfigurationExtractor = springLockConfigurationExtractor;
    }

    @Override
    public Optional<LockConfiguration> getLockConfiguration(Object target, Method method, Object[] parameterValues) {
        Optional<LockConfiguration> lockConfiguration = springLockConfigurationExtractor.getLockConfiguration(target, method, parameterValues);
        if (lockConfiguration.isPresent()) {
            DistributedLock annotation = AnnotationUtils.getAnnotation(method, DistributedLock.class);
            if (annotation == null) {
                return lockConfiguration;
            } else {
                LockConfiguration configuration = lockConfiguration.get();
                ScopedLockConfiguration scopedLockConfiguration = ScopedLockConfiguration.builder(annotation.scope())
                        .throwOnLockFailure(annotation.throwOnLockFailure())
                        .name(configuration.getName())
                        .lockAtMostFor(configuration.getLockAtMostFor())
                        .lockAtLeastFor(configuration.getLockAtLeastFor())
                        .waitForLock(getDurationValue(annotation.waitForLock()))
                        .build();
                return Optional.of(scopedLockConfiguration);
            }
        } else {
            return Optional.empty();
        }
    }

    private Duration getDurationValue(String value) {
        return ObjectUtils.isEmpty(value) ? null : durationConverter.convert(value);
    }

    @Override
    public Optional<LockConfiguration> getLockConfiguration(Runnable task) {
        return springLockConfigurationExtractor.getLockConfiguration(task);
    }
}
