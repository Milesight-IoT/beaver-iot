package com.milesight.beaveriot.rule.components.timer;

import com.cronutils.model.time.ExecutionTime;
import lombok.extern.slf4j.*;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;
import org.springframework.util.Assert;

import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


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

        if (SimpleTimerSettings.TimerType.ONCE.equals(settings.getType())) {
            return getRule(settings.getTimezone(), settings.getExecutionEpochSecond());
        } else {
            return getRule(settings.getTimezone(), settings.getRules(), settings.getExpirationEpochSecond());
        }
    }

    private static SimpleTimerRule getRule(String timezone, Long executionEpochSecond) {
        return getRule(timezone, null, Collections.singletonList(executionEpochSecond), null);
    }

    private static SimpleTimerRule getRule(String timezone,
                                           List<SimpleTimerRuleSettings> rules,
                                           Long expirationEpochSecond) {
        return getRule(timezone, rules, null, expirationEpochSecond);
    }

    private static SimpleTimerRule getRule(String timezone,
                                           List<SimpleTimerRuleSettings> rules,
                                           List<Long> externalExecutionEpochSeconds,
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
        return new SimpleTimerRule(tz, executionTimes, externalExecutionEpochSeconds, expirationEpochSecond);
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
