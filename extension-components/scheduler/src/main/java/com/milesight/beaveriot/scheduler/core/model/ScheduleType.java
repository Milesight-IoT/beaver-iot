package com.milesight.beaveriot.scheduler.core.model;

import lombok.*;


@Getter
@RequiredArgsConstructor
public enum ScheduleType {
    ONCE,
    FIXED_RATE,
    CRON,
    ;

    @Override
    public String toString() {
        return name();
    }
}
