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
public class ScheduledTaskCallbackTerminatedEvent extends RemoteBroadcastMessage {

    private String tenantId;

    private List<String> taskKeys;

    public ScheduledTaskCallbackTerminatedEvent(List<String> taskKeys) {
        this.taskKeys = taskKeys;
    }

}
