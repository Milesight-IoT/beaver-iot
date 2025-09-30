package com.milesight.beaveriot.blueprint.library.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibrarySubscription;
import com.milesight.beaveriot.blueprint.library.po.BlueprintLibrarySubscriptionPO;
import com.milesight.beaveriot.blueprint.library.repository.BlueprintLibrarySubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/19 10:55
 **/
@Service
public class BlueprintLibrarySubscriptionService {
    private final BlueprintLibrarySubscriptionRepository blueprintLibrarySubscriptionRepository;

    public BlueprintLibrarySubscriptionService(BlueprintLibrarySubscriptionRepository blueprintLibrarySubscriptionRepository) {
        this.blueprintLibrarySubscriptionRepository = blueprintLibrarySubscriptionRepository;
    }

    public List<BlueprintLibrarySubscription> findAllIgnoreTenant() {
        return blueprintLibrarySubscriptionRepository.findAllIgnoreTenant()
                .stream()
                .map(this::convertPOtoModel)
                .toList();
    }

    public List<BlueprintLibrarySubscription> findAll() {
        return blueprintLibrarySubscriptionRepository.findAll()
                .stream()
                .map(this::convertPOtoModel)
                .toList();
    }

    public BlueprintLibrarySubscription findByLibraryId(Long libraryId) {
        return blueprintLibrarySubscriptionRepository.findAllByLibraryId(libraryId)
                .stream()
                .map(this::convertPOtoModel)
                .findFirst()
                .orElse(null);
    }

    public void save(BlueprintLibrarySubscription model) {
        blueprintLibrarySubscriptionRepository.save(convertModelToPO(model));
    }

    public List<BlueprintLibrarySubscription> findAllByActiveTrueIgnoreTenant() {
        return blueprintLibrarySubscriptionRepository.findAllByActiveTrueIgnoreTenant()
                .stream()
                .map(this::convertPOtoModel)
                .toList();
    }

    public BlueprintLibrarySubscription findByActiveTrue() {
        return blueprintLibrarySubscriptionRepository.findAllByActiveTrue()
                .stream()
                .map(this::convertPOtoModel)
                .findFirst()
                .orElse(null);
    }

    @Transactional
    public void setActiveOnlyByLibraryId(Long libraryId) {
        blueprintLibrarySubscriptionRepository.setActiveOnlyByLibraryId(libraryId);
    }

    public BlueprintLibrarySubscriptionPO convertModelToPO(BlueprintLibrarySubscription model) {
        if (model.getId() == null) {
            model.setId(SnowflakeUtil.nextId());
        }

        BlueprintLibrarySubscriptionPO po = new BlueprintLibrarySubscriptionPO();
        po.setId(model.getId());
        po.setLibraryId(model.getLibraryId());
        po.setLibraryVersion(model.getLibraryVersion());
        po.setActive(model.getActive());
        return po;
    }

    public BlueprintLibrarySubscription convertPOtoModel(BlueprintLibrarySubscriptionPO po) {
        return BlueprintLibrarySubscription.builder()
                .id(po.getId())
                .libraryId(po.getLibraryId())
                .libraryVersion(po.getLibraryVersion())
                .active(po.getActive())
                .tenantId(po.getTenantId())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByLibraryIdAndLibraryVersionIgnoreTenant(Long libraryId, String libraryVersion) {
        blueprintLibrarySubscriptionRepository.deleteByLibraryIdAndLibraryVersionIgnoreTenant(libraryId, libraryVersion);
    }
}
