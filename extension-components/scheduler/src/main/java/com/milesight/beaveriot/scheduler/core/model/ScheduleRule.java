package com.milesight.beaveriot.scheduler.core.model;

import lombok.*;

import java.util.Objects;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleRule {

    private Set<String> cronExpressions;

    private Long periodSecond;

    private Long startEpochSecond;

    private Long expirationEpochSecond;

    private String timezone;

    public boolean equalsWith(ScheduleRule scheduleRule) {
        if (this == scheduleRule) {
            return true;
        }
        if (!Objects.equals(scheduleRule.cronExpressions, cronExpressions)) {
            return false;
        }
        if (!Objects.equals(scheduleRule.periodSecond, periodSecond)) {
            return false;
        }
        if (!Objects.equals(scheduleRule.startEpochSecond, startEpochSecond)) {
            return false;
        }
        if (!Objects.equals(scheduleRule.expirationEpochSecond, expirationEpochSecond)) {
            return false;
        }
        return Objects.equals(scheduleRule.timezone, timezone);
    }

}
