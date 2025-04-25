package com.milesight.beaveriot.scheduler.core.model;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

import java.util.Objects;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleSettings {

    @NonNull
    private ScheduleType scheduleType;

    @NotNull
    private ScheduleRule scheduleRule;

    @Nullable
    private String payload;

    public boolean equalsWith(ScheduleSettings scheduleSettings) {
        if (this == scheduleSettings) {
            return true;
        }
        if (!Objects.equals(scheduleSettings.scheduleType, scheduleType)) {
            return false;
        }
        if (!scheduleSettings.scheduleRule.equalsWith(scheduleRule)) {
            return false;
        }
        return Objects.equals(scheduleSettings.payload, payload);
    }

}
