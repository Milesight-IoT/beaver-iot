package com.milesight.beaveriot.context.integration.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Credentials {

    private Long id;

    private String credentialsType;

    private String accessKey;

    private String accessSecret;

    private String additionalData;

}
