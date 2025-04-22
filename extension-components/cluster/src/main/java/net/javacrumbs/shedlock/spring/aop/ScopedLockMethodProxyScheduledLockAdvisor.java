package net.javacrumbs.shedlock.spring.aop;

import com.milesight.beaveriot.base.exception.AcquiredLockException;
import net.javacrumbs.shedlock.core.*;
import net.javacrumbs.shedlock.spring.ExtendedLockConfigurationExtractor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import net.javacrumbs.shedlock.support.annotation.Nullable;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractPointcutAdvisor;
import org.springframework.aop.support.ComposablePointcut;
import org.springframework.aop.support.annotation.AnnotationMatchingPointcut;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.lang.annotation.Annotation;
import java.util.Optional;

/**
 * @author leon
 */
public class ScopedLockMethodProxyScheduledLockAdvisor extends AbstractPointcutAdvisor {
    private final Pointcut pointcut = new ComposablePointcut(methodPointcutFor(SchedulerLock.class));

    private final Advice advice;

    ScopedLockMethodProxyScheduledLockAdvisor(ConfigurableListableBeanFactory beanFactory) {
        this.advice = new LockingInterceptor(beanFactory);
    }

    private static AnnotationMatchingPointcut methodPointcutFor(Class<? extends Annotation> methodAnnotationType) {
        return new AnnotationMatchingPointcut(null, methodAnnotationType, true);
    }

    /** Get the Pointcut that drives this advisor. */
    @Override
    public Pointcut getPointcut() {
        return pointcut;
    }

    @Override
    public Advice getAdvice() {
        return advice;
    }

    private static class LockingInterceptor implements MethodInterceptor {
        private final ConfigurableListableBeanFactory beanFactory;
        private ExtendedLockConfigurationExtractor lockConfigurationExtractor;
        private final LockProviderSupplier lockProviderSupplier;

        LockingInterceptor(ConfigurableListableBeanFactory beanFactory) {
            this.lockProviderSupplier = LockProviderSupplier.create(beanFactory);
            this.beanFactory = beanFactory;
        }

        @Override
        @Nullable
        public Object invoke(MethodInvocation invocation) throws Throwable {
            Class<?> returnType = invocation.getMethod().getReturnType();
            if (returnType.isPrimitive() && !void.class.equals(returnType)) {
                throw new LockingNotSupportedException("Can not lock method returning primitive value");
            }

            ScopedLockConfiguration lockConfiguration = (ScopedLockConfiguration) getLockConfigurationExtractor()
                    .getLockConfiguration(invocation.getThis(), invocation.getMethod(), invocation.getArguments())
                    .get();

            LockProvider lockProvider = lockProviderSupplier.supply(
                    invocation.getThis(), invocation.getMethod(), invocation.getArguments());
            LockProvider retryableLockProvider = lockProvider instanceof RetryableLockProvider ? lockProvider : new RetryableLockProvider(lockProvider);
            DefaultLockingTaskExecutor lockingTaskExecutor = new DefaultLockingTaskExecutor(retryableLockProvider);
            LockingTaskExecutor.TaskResult<Object> result = lockingTaskExecutor.executeWithLock(invocation::proceed, lockConfiguration);

            boolean throwOnLockFailure = getThrowOnLockFailure(lockConfiguration);
            if (throwOnLockFailure && !result.wasExecuted()) {
                 throw new AcquiredLockException("Could not acquire distributed lock.");
            }
            if (Optional.class.equals(returnType)) {
                return toOptional(result);
            } else {
                return result.getResult();
            }
        }

        private boolean getThrowOnLockFailure(LockConfiguration lockConfiguration) {
            return lockConfiguration instanceof ScopedLockConfiguration scopedLockConfiguration
                    && scopedLockConfiguration.isThrowOnLockFailure();
        }

        private ExtendedLockConfigurationExtractor getLockConfigurationExtractor() {
            if (lockConfigurationExtractor == null) {
                this.lockConfigurationExtractor = beanFactory.getBean(ExtendedLockConfigurationExtractor.class);
            }
            return this.lockConfigurationExtractor;
        }

        @Nullable
        private static Object toOptional(LockingTaskExecutor.TaskResult<Object> result) {
            if (result.wasExecuted()) {
                return result.getResult();
            } else {
                return Optional.empty();
            }
        }
    }
}
