package com.milesight.beaveriot.scheduler.core.model;

import com.milesight.beaveriot.pubsub.api.message.LocalUnicastMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledTaskUpdatedEvent extends LocalUnicastMessage {

    private ScheduledTask scheduledTask;

    private List<Long> previousTaskIds;

    public ScheduledTaskUpdatedEvent(String tenantId, ScheduledTask scheduledTask, List<Long> previousTaskIds) {
        super(tenantId);
        this.scheduledTask = scheduledTask;
        this.previousTaskIds = previousTaskIds;
    }

}
