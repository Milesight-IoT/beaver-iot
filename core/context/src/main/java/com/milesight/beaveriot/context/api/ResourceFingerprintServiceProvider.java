package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.integration.model.ResourceFingerprint;

/**
 * author: Luxb
 * create: 2025/9/3 16:32
 **/
public interface ResourceFingerprintServiceProvider {
    ResourceFingerprint getResourceFingerprint(String type, String integration);
    void save(ResourceFingerprint resourceFingerprint);
}
