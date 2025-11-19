package com.milesight.beaveriot.credentials.api.model;

import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.pubsub.api.message.LocalUnicastMessage;
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
public class CredentialsChangeEvent extends LocalUnicastMessage {

    @NonNull
    private Operation operation;

    @NonNull
    private Credentials credentials;

    @NonNull
    private Long timestamp;

    public enum Operation {
        ADD,
        DELETE
    }

}
