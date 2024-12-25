package com.milesight.beaveriot.rule.components.timer;

import com.cronutils.model.time.ExecutionTime;
import lombok.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.List;
import java.util.TreeSet;

@Data
@AllArgsConstructor
public class SimpleTimerRule {

    private String timezone;

    private List<ExecutionTime> executionTimes;

    private List<Long> externalExecutionEpochSeconds;

    private Long expirationEpochSecond;

    public Long nextExecutionEpochSecond() {
        return nextExecutionEpochSecond(ZonedDateTime.now(ZoneId.of(timezone)));
    }

    public Long nextExecutionEpochSecond(ZonedDateTime now) {
        if (expirationEpochSecond == null) {
            expirationEpochSecond = Long.MAX_VALUE;
        }
        var nowEpochSecond = now.toEpochSecond();
        var nextExecutionEpochSeconds = new TreeSet<Long>();
        if (externalExecutionEpochSeconds != null && !externalExecutionEpochSeconds.isEmpty()) {
            externalExecutionEpochSeconds.stream()
                    .filter(t -> t > nowEpochSecond && t < expirationEpochSecond)
                    .forEach(nextExecutionEpochSeconds::add);
        }
        if (executionTimes != null && !executionTimes.isEmpty()) {
            executionTimes.forEach(t -> t.nextExecution(now)
                    .map(ChronoZonedDateTime::toEpochSecond)
                    .map(s -> s < expirationEpochSecond ? s : null)
                    .ifPresent(nextExecutionEpochSeconds::add));
        }
        return nextExecutionEpochSeconds.pollFirst();
    }

}
