package com.milesight.beaveriot.blueprint.library.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryVersion;
import com.milesight.beaveriot.blueprint.library.po.BlueprintLibraryVersionPO;
import com.milesight.beaveriot.blueprint.library.repository.BlueprintLibraryVersionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * author: Luxb
 * create: 2025/9/19 10:19
 **/
@Service
public class BlueprintLibraryVersionService {
    private final BlueprintLibraryVersionRepository blueprintLibraryVersionRepository;

    public BlueprintLibraryVersionService(BlueprintLibraryVersionRepository blueprintLibraryVersionRepository) {
        this.blueprintLibraryVersionRepository = blueprintLibraryVersionRepository;
    }

    public BlueprintLibraryVersion findByLibraryIdAndLibraryVersion(Long libraryId, String libraryVersion) {
        return blueprintLibraryVersionRepository.findAllByLibraryIdAndLibraryVersion(libraryId, libraryVersion)
                .stream()
                .map(this::convertPOtoModel)
                .findFirst()
                .orElse(null);
    }

    public List<BlueprintLibraryVersion> findAllByLibraryId(Long libraryId) {
        return blueprintLibraryVersionRepository.findAllByLibraryId(libraryId)
                .stream()
                .map(this::convertPOtoModel)
                .toList();
    }

    public void save(BlueprintLibraryVersion model) {
        blueprintLibraryVersionRepository.save(convertModelToPO(model));
    }

    public BlueprintLibraryVersionPO convertModelToPO(BlueprintLibraryVersion model) {
        if (model.getId() == null) {
            model.setId(SnowflakeUtil.nextId());
        }

        BlueprintLibraryVersionPO po = new BlueprintLibraryVersionPO();
        po.setId(model.getId());
        po.setLibraryId(model.getLibraryId());
        po.setLibraryVersion(model.getLibraryVersion());
        po.setSyncedAt(model.getSyncedAt());
        return po;
    }

    public BlueprintLibraryVersion convertPOtoModel(BlueprintLibraryVersionPO po) {
        return BlueprintLibraryVersion.builder()
                .id(po.getId())
                .libraryId(po.getLibraryId())
                .libraryVersion(po.getLibraryVersion())
                .syncedAt(po.getSyncedAt())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByLibraryIdAndLibraryVersion(Long libraryId, String libraryVersion) {
        blueprintLibraryVersionRepository.deleteByLibraryIdAndLibraryVersion(libraryId, libraryVersion);
    }

    public Long countByLibraryId(Long libraryId) {
        return blueprintLibraryVersionRepository.countByLibraryId(libraryId);
    }

    public List<BlueprintLibraryVersion> findAll() {
        return blueprintLibraryVersionRepository.findAll()
                .stream()
                .map(this::convertPOtoModel)
                .toList();
    }
}