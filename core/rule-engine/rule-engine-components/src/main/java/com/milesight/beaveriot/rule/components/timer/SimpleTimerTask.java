package com.milesight.beaveriot.rule.components.timer;

import lombok.*;
import lombok.extern.slf4j.*;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Data
public class SimpleTimerTask {

    private String id;

    private SimpleTimerConsumer consumer;

    private SimpleTimerRule rule;

    private Long executionEpochSecond;

    private boolean executed = false;

    public SimpleTimerTask(String id, SimpleTimerConsumer consumer, SimpleTimerRule rule) {
        this.id = id;
        this.consumer = consumer;
        this.rule = rule;
        this.executionEpochSecond = rule.nextExecutionEpochSecond();
    }

    private SimpleTimerTask(String id, SimpleTimerConsumer consumer, SimpleTimerRule rule, long executionEpochSecond) {
        this.id = id;
        this.consumer = consumer;
        this.rule = rule;
        this.executionEpochSecond = executionEpochSecond;
    }

    public Optional<SimpleTimerTask> nextExecution() {
        var nextExecutionEpochSecond = rule.nextExecutionEpochSecond();
        if (nextExecutionEpochSecond == null) {
            return Optional.empty();
        }
        return Optional.of(new SimpleTimerTask(id, consumer, rule, nextExecutionEpochSecond));
    }

    public void run() {
        log.info("run task: {}", id);
        try {
            var exchange = consumer.getEndpoint().createExchange();
            exchange.getIn().setBody(Map.of("executionEpochSecond", executionEpochSecond, "timezone", rule.getTimezone()));
            consumer.getProcessor().process(exchange);
        } catch (Exception e) {
            log.error("run task failed: {}", id, e);
        }
    }

}
