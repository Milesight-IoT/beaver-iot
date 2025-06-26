package com.milesight.beaveriot.metrics.autoconfigure;

import io.micrometer.core.aop.CountedAspect;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RestController;

/**
 * author: Luxb
 * create: 2025/6/26 9:36
 **/
@Configuration
public class MicrometerAutoConfiguration {
    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry, this::skipControllers);
    }

    @Bean
    public CountedAspect countedAspect(MeterRegistry meterRegistry) {
        return new CountedAspect(meterRegistry, this::skipControllers);
    }

    private boolean skipControllers(ProceedingJoinPoint pjp) {
        Class<?> targetClass = pjp.getTarget().getClass();
        return targetClass.isAnnotationPresent(RestController.class) || targetClass.isAnnotationPresent(Controller.class);
    }
}
