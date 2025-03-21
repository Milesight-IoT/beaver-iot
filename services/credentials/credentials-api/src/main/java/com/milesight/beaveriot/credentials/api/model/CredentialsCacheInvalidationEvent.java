package com.milesight.beaveriot.credentials.api.model;

import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage;
import lombok.*;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
public class CredentialsCacheInvalidationEvent extends RemoteBroadcastMessage {

    private String tenantId;

    @NonNull
    private Credentials credentials;

    @NonNull
    private Long timestamp;

    public CredentialsCacheInvalidationEvent(@NonNull Credentials credentials, @NonNull Long timestamp) {
        this.credentials = credentials;
        this.timestamp = timestamp;
    }

}
