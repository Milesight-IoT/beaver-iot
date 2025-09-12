package com.milesight.beaveriot.resource.manager.model;

import lombok.Builder;
import lombok.Data;

/**
 * author: Luxb
 * create: 2025/9/3 16:22
 **/
@Builder
@Data
public class ResourceFingerprint {
    private Long id;
    private String type;
    private String integration;
    private String hash;
}
