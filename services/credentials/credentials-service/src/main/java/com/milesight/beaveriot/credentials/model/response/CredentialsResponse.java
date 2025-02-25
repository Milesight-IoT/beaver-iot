package com.milesight.beaveriot.credentials.model.response;

import com.fasterxml.jackson.annotation.JsonRawValue;
import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsResponse {

    private String id;

    private String tenantId;

    private String credentialsType;

    private String description;

    private String accessKey;

    private String accessSecret;

    @JsonRawValue
    private String additionalData;

    private Boolean editable;

    private Long createdAt;

    private Long updatedAt;

}
