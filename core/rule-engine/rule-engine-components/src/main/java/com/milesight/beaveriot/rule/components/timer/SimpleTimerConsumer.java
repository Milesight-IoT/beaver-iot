package com.milesight.beaveriot.rule.components.timer;

import com.cronutils.model.time.ExecutionTime;
import lombok.extern.slf4j.*;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;


@Slf4j
public class SimpleTimerConsumer extends DefaultConsumer {

    public SimpleTimerConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
        SimpleTimerScheduler.instance().scheduleTask(this, getRule());
    }

    public SimpleTimerRule getRule() {
        var settings = getEndpoint().getTimerSettings();
        Objects.requireNonNull(settings, "timer settings is null");
        Objects.requireNonNull(settings.getType(), "timer settings type is null");

        return switch (settings.getType()) {
            case ONCE -> getRule(settings.getTimezone(), settings.getExecutionEpochSecond());
            case INTERVAL -> getRule(settings.getTimezone(), settings.getIntervalTimeUnit(), settings.getIntervalTime());
            default -> getRule(settings.getTimezone(), settings.getRules(), settings.getExpirationEpochSecond());
        };
    }

    private static SimpleTimerRule getRule(String timezone, Long executionEpochSecond) {
        return getRule(timezone, null, Collections.singletonList(executionEpochSecond), null, null);
    }

    private static SimpleTimerRule getRule(String timezone, TimeUnit intervalTimeUnit, Long intervalTime) {
        if (intervalTimeUnit == null || intervalTimeUnit.ordinal() < TimeUnit.MINUTES.ordinal()) {
            throw new IllegalArgumentException("invalid intervalTimeUnit");
        }
        if (intervalTime == null || intervalTime < 1) {
            throw new IllegalArgumentException("invalid intervalTime");
        }
        var intervalSeconds = intervalTimeUnit.toSeconds(intervalTime);
        return getRule(timezone, null, null, Collections.singletonList(intervalSeconds), null);
    }

    private static SimpleTimerRule getRule(String timezone,
                                           List<SimpleTimerRuleSettings> rules,
                                           Long expirationEpochSecond) {
        return getRule(timezone, rules, null, null, expirationEpochSecond);
    }

    private static SimpleTimerRule getRule(String timezone,
                                           List<SimpleTimerRuleSettings> rules,
                                           List<Long> externalExecutionEpochSeconds,
                                           List<Long> intervalSeconds,
                                           Long expirationEpochSecond) {
        var zoneId = Optional.ofNullable(timezone)
                .map(ZoneId::of)
                .orElse(ZoneId.systemDefault());
        var tz = zoneId.toString();

        var executionTimes = rules != null
                ? rules.stream()
                .map(r -> ExecutionTime.forCron(r.toCron()))
                .toList()
                : null;
        return new SimpleTimerRule(tz, executionTimes, externalExecutionEpochSeconds, intervalSeconds, expirationEpochSecond);
    }

    public String getId() {
        return getEndpoint().getFlowId();
    }

    @Override
    public SimpleTimerEndpoint getEndpoint() {
        return (SimpleTimerEndpoint) super.getEndpoint();
    }

    @Override
    protected void doStop() throws Exception {
        log.info("SimpleTimerConsumer doStop");
        super.doStop();
    }

}
