package com.milesight.beaveriot.context.model;

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

    public static ResourceRefDTO of(String refId, String refType) {
        return new ResourceRefDTO(refId, refType);
    }
}
