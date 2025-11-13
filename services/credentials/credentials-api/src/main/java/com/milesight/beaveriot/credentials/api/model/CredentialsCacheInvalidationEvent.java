package com.milesight.beaveriot.credentials.api.model;

import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.ToString;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CredentialsCacheInvalidationEvent extends RemoteBroadcastMessage {

    @NonNull
    private Credentials credentials;

    @NonNull
    private Long timestamp;

}
