package com.milesight.beaveriot.resource.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * ResourcePersistDTO class.
 *
 * @author simon
 * @date 2025/4/14
 */
@Data
@AllArgsConstructor
public class ResourceRefDTO {
    private String refId;

    private String refType;
}
