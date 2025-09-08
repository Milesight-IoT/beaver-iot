package com.milesight.beaveriot.resource.manager.repository;

import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.resource.manager.po.ResourceFingerprintPO;

/**
 * author: Luxb
 * create: 2025/9/3 16:19
 **/
public interface ResourceFingerprintRepository extends BaseJpaRepository<ResourceFingerprintPO, Long> {
    ResourceFingerprintPO findByTypeAndIntegration(String type, String integration);
}
