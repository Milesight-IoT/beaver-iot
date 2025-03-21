package com.milesight.beaveriot.credentials.api.model;

import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.pubsub.api.message.LocalUnicastMessage;
import lombok.*;


@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class CredentialsChangeEvent extends LocalUnicastMessage {

    private String tenantId;

    @NonNull
    private Operation operation;

    @NonNull
    private Credentials credentials;

    @NonNull
    private Long timestamp;

    public CredentialsChangeEvent(@NonNull Operation operation, @NonNull Credentials credentials, @NonNull Long timestamp) {
        this.credentials = credentials;
        this.operation = operation;
        this.timestamp = timestamp;
    }

    public enum Operation {
        ADD,
        DELETE
    }

}
