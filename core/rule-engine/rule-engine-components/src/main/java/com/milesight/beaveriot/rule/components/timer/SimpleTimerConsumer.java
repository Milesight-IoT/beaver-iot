package com.milesight.beaveriot.rule.components.timer;

import com.cronutils.model.Cron;
import com.milesight.beaveriot.scheduler.core.Scheduler;
import com.milesight.beaveriot.scheduler.core.model.ScheduleRule;
import com.milesight.beaveriot.scheduler.core.model.ScheduleSettings;
import com.milesight.beaveriot.scheduler.core.model.ScheduleType;
import lombok.extern.slf4j.*;
import lombok.*;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

import java.time.ZoneId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


@Slf4j
public class SimpleTimerConsumer extends DefaultConsumer {

    private final Scheduler scheduler;

    public SimpleTimerConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
        scheduler = ((SimpleTimerComponent) getEndpoint().getComponent()).getScheduler();
        doSchedule();
    }

    private void doSchedule() {
        val taskKey = getTaskKey();
        scheduler.schedule(taskKey, getScheduleSettings(), scheduledTask -> {
            log.info("run task: {}", scheduledTask.getTaskKey());
            try {
                var exchange = getEndpoint().createExchange();
                exchange.getIn().setBody(Map.of(
                        "executionEpochSecond", scheduledTask.getExecutionEpochSecond(),
                        "timezone", scheduledTask.getScheduleSettings().getScheduleRule().getTimezone()));
                getProcessor().process(exchange);
            } catch (Exception e) {
                log.error("run task failed: {}", scheduledTask.getTaskKey(), e);
            }
        });
    }

    public ScheduleSettings getScheduleSettings() {
        var settings = getEndpoint().getTimerSettings();
        Objects.requireNonNull(settings, "timer settings is null");
        Objects.requireNonNull(settings.getType(), "timer settings type is null");

        var zoneId = Optional.ofNullable(settings.getTimezone())
                .map(ZoneId::of)
                .orElse(ZoneId.systemDefault());
        var timezone = zoneId.getId();
        return switch (settings.getType()) {
            case ONCE -> ScheduleSettings.builder()
                    .scheduleType(ScheduleType.ONCE)
                    .scheduleRule(ScheduleRule.builder()
                            .startEpochSecond(settings.getExecutionEpochSecond())
                            .timezone(timezone)
                            .build())
                    .build();
            case INTERVAL -> {
                if (settings.getIntervalTimeUnit() == null
                        || settings.getIntervalTimeUnit().ordinal() < TimeUnit.SECONDS.ordinal()
                        || (settings.getIntervalTimeUnit().ordinal() == TimeUnit.SECONDS.ordinal() && settings.getIntervalTime() < 10)
                ) {
                    throw new IllegalArgumentException("invalid intervalTimeUnit");
                }
                if (settings.getIntervalTime() == null || settings.getIntervalTime() < 1) {
                    throw new IllegalArgumentException("invalid intervalTime");
                }
                yield ScheduleSettings.builder()
                        .scheduleType(ScheduleType.FIXED_RATE)
                        .scheduleRule(ScheduleRule.builder()
                                .periodSecond(settings.getIntervalTimeUnit().toSeconds(settings.getIntervalTime()))
                                .expirationEpochSecond(settings.getExpirationEpochSecond())
                                .timezone(timezone)
                                .build())
                        .build();
            }
            case SCHEDULE -> ScheduleSettings.builder()
                    .scheduleType(ScheduleType.CRON)
                    .scheduleRule(ScheduleRule.builder()
                            .cronExpressions(settings.getRules().stream()
                                    .map(SimpleTimerRuleSettings::toCron)
                                    .map(Cron::asString)
                                    .collect(Collectors.toSet()))
                            .expirationEpochSecond(settings.getExpirationEpochSecond())
                            .timezone(timezone)
                            .build())
                    .build();
        };
    }

    public String getId() {
        return getEndpoint().getFlowId();
    }

    public String getTaskKey() {
        return "workflow:simple-timer:" + getId();
    }

    @Override
    public SimpleTimerEndpoint getEndpoint() {
        return (SimpleTimerEndpoint) super.getEndpoint();
    }

    @Override
    protected void doStop() throws Exception {
        log.info("SimpleTimerConsumer doStop");
        scheduler.removeCallback(getTaskKey());
        super.doStop();
    }

}
