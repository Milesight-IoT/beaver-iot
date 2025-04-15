package com.milesight.beaveriot.resource.model;

import lombok.Data;

/**
 * PreSignResult class.
 *
 * @author simon
 * @date 2025/4/3
 */
@Data
public class PreSignResult {
    private String uploadUrl;

    private String resourceUrl;

    private String key;
}
