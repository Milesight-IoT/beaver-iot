package com.milesight.beaveriot.pubsub.api.message;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;


@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class RemoteBroadcastMessage implements PubSubMessage {

    protected String tenantId;

}
