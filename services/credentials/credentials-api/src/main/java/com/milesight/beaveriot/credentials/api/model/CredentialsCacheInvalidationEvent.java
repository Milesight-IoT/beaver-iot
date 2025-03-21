package com.milesight.beaveriot.credentials.api.model;

import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage;
import lombok.*;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsCacheInvalidationEvent extends RemoteBroadcastMessage {

    @NonNull
    private Credentials credentials;

    @NonNull
    private Long timestamp;

}
