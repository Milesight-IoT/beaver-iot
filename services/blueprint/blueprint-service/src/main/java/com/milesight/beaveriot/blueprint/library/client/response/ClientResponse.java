package com.milesight.beaveriot.blueprint.library.client.response;

import lombok.Builder;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/6 10:23
 **/
@Builder
@Data
public class ClientResponse {
    private int code;
    private String message;
    private String data;
    private boolean isSuccessful;
}
