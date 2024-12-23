package com.milesight.beaveriot.rule.components.timer;

import lombok.*;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleTimerSettings {

    private TimerType type;

    private String timezone;

    private Long executionEpochSecond;

    private List<SimpleTimerRuleSettings> rules;

    private Long expirationEpochSecond;

    public enum TimerType {
        ONCE,
        SCHEDULE
    }

}
