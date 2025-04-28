package com.milesight.beaveriot.scheduler.core.model;

import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage;
import lombok.*;

import java.time.ZonedDateTime;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTaskRemoteTriggeredEvent extends RemoteBroadcastMessage {

    private String tenantId;

    private ScheduledTask scheduledTask;

    private ZonedDateTime taskExecutionDateTime;

}
