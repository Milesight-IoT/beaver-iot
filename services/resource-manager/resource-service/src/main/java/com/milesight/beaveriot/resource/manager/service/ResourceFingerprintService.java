package com.milesight.beaveriot.resource.manager.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.resource.manager.facade.IResourceFingerprintFacade;
import com.milesight.beaveriot.resource.manager.model.ResourceFingerprint;
import com.milesight.beaveriot.resource.manager.po.ResourceFingerprintPO;
import com.milesight.beaveriot.resource.manager.repository.ResourceFingerprintRepository;
import org.springframework.stereotype.Service;

/**
 * author: Luxb
 * create: 2025/9/3 16:18
 **/
@Service
public class ResourceFingerprintService implements IResourceFingerprintFacade {
    private final ResourceFingerprintRepository resourceFingerprintRepository;

    public ResourceFingerprintService(ResourceFingerprintRepository resourceFingerprintRepository) {
        this.resourceFingerprintRepository = resourceFingerprintRepository;
    }

    public ResourceFingerprint getResourceFingerprint(String type, String integration) {
        return convertPOToModel(resourceFingerprintRepository.findByTypeAndIntegration(type, integration));
    }

    public void save(ResourceFingerprint resourceFingerprint) {
        resourceFingerprintRepository.save(convertModelToPO(resourceFingerprint));
    }

    public ResourceFingerprint convertPOToModel(ResourceFingerprintPO resourceFingerprintPO) {
        if (resourceFingerprintPO == null) {
            return null;
        }

        return ResourceFingerprint.builder()
                .id(resourceFingerprintPO.getId())
                .type(resourceFingerprintPO.getType())
                .integration(resourceFingerprintPO.getIntegration())
                .hash(resourceFingerprintPO.getHash())
                .build();
    }

    public ResourceFingerprintPO convertModelToPO(ResourceFingerprint resourceFingerprint) {
        if (resourceFingerprint == null) {
            return null;
        }

        ResourceFingerprintPO resourceFingerprintPO = new ResourceFingerprintPO();
        Long id = resourceFingerprint.getId();
        if (id == null) {
            id = SnowflakeUtil.nextId();
            resourceFingerprintPO.setCreatedAt(System.currentTimeMillis());
        }
        resourceFingerprintPO.setId(id);
        resourceFingerprintPO.setType(resourceFingerprint.getType());
        resourceFingerprintPO.setIntegration(resourceFingerprint.getIntegration());
        resourceFingerprintPO.setHash(resourceFingerprint.getHash());

        return resourceFingerprintPO;
    }
}
