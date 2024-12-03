package com.milesight.beaveriot.integration.model.request;

import lombok.Data;

@Data
public class SearchIntegrationRequest {
    private Boolean deviceAddable;

    private Boolean deviceDeletable;
}
