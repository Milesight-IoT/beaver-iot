package com.milesight.beaveriot.resource.manager.facade;

import com.milesight.beaveriot.resource.manager.model.ResourceFingerprint;

/**
 * author: Luxb
 * create: 2025/9/3 16:32
 **/
public interface IResourceFingerprintFacade {
    ResourceFingerprint getResourceFingerprint(String type, String integration);
    void save(ResourceFingerprint resourceFingerprint);
}
