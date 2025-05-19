package com.milesight.beaveriot.rule.components.timer;

import lombok.*;

import java.time.DayOfWeek;
import java.util.List;

@Getter
@RequiredArgsConstructor
public enum DayOfWeekOptions {
    MONDAY(List.of(DayOfWeek.MONDAY)),
    TUESDAY(List.of(DayOfWeek.TUESDAY)),
    WEDNESDAY(List.of(DayOfWeek.WEDNESDAY)),
    THURSDAY(List.of(DayOfWeek.THURSDAY)),
    FRIDAY(List.of(DayOfWeek.FRIDAY)),
    SATURDAY(List.of(DayOfWeek.SATURDAY)),
    SUNDAY(List.of(DayOfWeek.SUNDAY)),
    EVERYDAY(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
    WEEKDAY(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)),
    WEEKEND(List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)),
    ;

    private final List<DayOfWeek> daysOfWeek;

    @Override
    public String toString() {
        return name();
    }
}
