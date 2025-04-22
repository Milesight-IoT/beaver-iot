package com.milesight.beaveriot.scheduler.core.model;

import lombok.*;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTask {

    @NonNull
    private Long id;

    @NonNull
    private Long executionEpochSecond;

    @NonNull
    private String taskKey;

    @NonNull
    private ScheduleSettings scheduleSettings;

    @Nullable
    private String tenantId;

    @NonNull
    private Integer attempts;

    @NonNull
    private Integer iteration;

    @NonNull
    private Long triggeredAt;

}
