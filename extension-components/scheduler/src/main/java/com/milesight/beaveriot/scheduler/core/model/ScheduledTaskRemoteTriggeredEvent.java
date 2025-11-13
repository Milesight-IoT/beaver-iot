package com.milesight.beaveriot.scheduler.core.model;

import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.ZonedDateTime;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTaskRemoteTriggeredEvent extends RemoteBroadcastMessage {

    private ScheduledTask scheduledTask;

    private ZonedDateTime taskExecutionDateTime;

}
