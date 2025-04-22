package com.milesight.beaveriot.scheduler.core.model;

import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage;
import lombok.*;

import java.util.List;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTaskCancelledEvent extends RemoteBroadcastMessage {

    private String tenantId;

    private String taskKey;

    private List<Long> taskIds;

    public ScheduledTaskCancelledEvent(String taskKey, List<Long> taskIds) {
        this.taskKey = taskKey;
        this.taskIds = taskIds;
    }

}
