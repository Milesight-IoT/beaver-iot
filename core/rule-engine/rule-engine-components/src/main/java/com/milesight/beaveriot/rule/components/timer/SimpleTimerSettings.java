package com.milesight.beaveriot.rule.components.timer;

import lombok.*;

import java.util.List;
import java.util.concurrent.TimeUnit;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTimerSettings {

    private TimerType type;

    private String timezone;

    private Long executionEpochSecond;

    private TimeUnit intervalTimeUnit;

    private Long intervalTime;

    private List<SimpleTimerRuleSettings> rules;

    private Long expirationEpochSecond;

    public enum TimerType {
        ONCE,
        INTERVAL,
        SCHEDULE
    }

}
