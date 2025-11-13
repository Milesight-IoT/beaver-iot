package com.milesight.beaveriot.rule.manager.model.event;

import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage;
import lombok.Data;

import java.util.UUID;

/**
 * @author leon
 */
@Data
public class BaseWorkflowEvent extends RemoteBroadcastMessage {

    public static String INSTANCE_UUID = UUID.randomUUID().toString();

    protected String id;

    protected String instanceId;

    public BaseWorkflowEvent() {
    }

    public BaseWorkflowEvent(String tenantId, String id) {
        this.tenantId = tenantId;
        this.id = id;
        this.instanceId = INSTANCE_UUID;
    }

    public boolean isSelfInstance() {
        return INSTANCE_UUID.equals(instanceId);
    }
}
