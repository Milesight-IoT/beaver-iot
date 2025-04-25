package com.milesight.beaveriot.scheduler.core.model;

import com.milesight.beaveriot.pubsub.api.message.LocalUnicastMessage;
import lombok.*;

import java.util.List;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTaskUpdatedEvent extends LocalUnicastMessage {

    private String tenantId;

    private ScheduledTask scheduledTask;

    private List<Long> previousTaskIds;

}
