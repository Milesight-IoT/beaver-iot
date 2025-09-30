package com.milesight.beaveriot.blueprint.library.service;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.blueprint.facade.IBlueprintLibraryFacade;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryAddress;
import com.milesight.beaveriot.blueprint.library.po.BlueprintLibraryPO;
import com.milesight.beaveriot.blueprint.library.repository.BlueprintLibraryRepository;
import com.milesight.beaveriot.context.model.BlueprintLibrary;
import com.milesight.beaveriot.context.model.BlueprintLibrarySourceType;
import com.milesight.beaveriot.context.model.BlueprintLibraryType;
import com.milesight.beaveriot.context.support.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * author: Luxb
 * create: 2025/9/1 9:41
 **/
@Slf4j
@Service
public class BlueprintLibraryService implements IBlueprintLibraryFacade {
    private final BlueprintLibraryRepository blueprintLibraryRepository;
    private final BlueprintLibraryAddressService blueprintLibraryAddressService;

    public BlueprintLibraryService(BlueprintLibraryRepository blueprintLibraryRepository, BlueprintLibraryAddressService blueprintLibraryAddressService) {
        this.blueprintLibraryRepository = blueprintLibraryRepository;
        this.blueprintLibraryAddressService = blueprintLibraryAddressService;
    }

    @Cacheable(cacheNames = Constants.CACHE_NAME_LIBRARY, key = "#p0 + ':' + #p1 + '@' + #p2", unless = "#result == null")
    public BlueprintLibrary getBlueprintLibrary(String type, String url, String branch) {
        if (StringUtils.isEmpty(url) || StringUtils.isEmpty(branch)) {
            return null;
        }

        BlueprintLibraryPO blueprintLibraryPO = blueprintLibraryRepository.findByTypeAndUrlAndBranch(type, url, branch);
        if (blueprintLibraryPO == null) {
            return null;
        }

        return convertPOToModel(blueprintLibraryPO);
    }

    @SuppressWarnings("unused")
    @CacheEvict(cacheNames = Constants.CACHE_NAME_LIBRARY, key = "#p0 + ':' + #p1 + '@' + #p2")
    public void evictCacheBlueprintLibrary(String type, String url, String branch) {
        log.debug("Evict cache: {}, key: {}:{}@{}",
                Constants.CACHE_NAME_LIBRARY,
                type,
                url,
                branch);
    }

    @Override
    public BlueprintLibrary findById(Long id) {
        return blueprintLibraryRepository.findById(id).map(this::convertPOToModel).orElse(null);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void deleteById(Long id) {
        blueprintLibraryRepository.deleteById(id);
    }

    @Override
    public BlueprintLibrary getCurrentBlueprintLibrary() {
        BlueprintLibraryAddress blueprintLibraryAddress = blueprintLibraryAddressService.getCurrentBlueprintLibraryAddress();
        return self().getBlueprintLibrary(blueprintLibraryAddress.getType().name(), blueprintLibraryAddress.getUrl(), blueprintLibraryAddress.getBranch());
    }

    public BlueprintLibraryService self() {
        return SpringContext.getBean(BlueprintLibraryService.class);
    }

    public void save(BlueprintLibrary blueprintLibrary) {
        BlueprintLibraryPO blueprintLibraryPO = convertModelToPO(blueprintLibrary);
        blueprintLibraryRepository.save(blueprintLibraryPO);
        blueprintLibrary.setId(blueprintLibraryPO.getId());
    }

    public BlueprintLibrary convertPOToModel(BlueprintLibraryPO blueprintLibraryPO) {
        return BlueprintLibrary.builder()
                .id(blueprintLibraryPO.getId())
                .type(BlueprintLibraryType.of(blueprintLibraryPO.getType()))
                .url(blueprintLibraryPO.getUrl())
                .branch(blueprintLibraryPO.getBranch())
                .currentVersion(blueprintLibraryPO.getCurrentVersion())
                .remoteVersion(blueprintLibraryPO.getRemoteVersion())
                .sourceType(BlueprintLibrarySourceType.of(blueprintLibraryPO.getSourceType()))
                .syncStatus(blueprintLibraryPO.getSyncStatus())
                .syncedAt(blueprintLibraryPO.getSyncedAt())
                .syncMessage(blueprintLibraryPO.getSyncMessage()).build();
    }

    public BlueprintLibraryPO convertModelToPO(BlueprintLibrary blueprintLibrary) {
        BlueprintLibraryPO blueprintLibraryPO = new BlueprintLibraryPO();
        long id;
        if (blueprintLibrary.getId() == null) {
            id = SnowflakeUtil.nextId();
        } else {
            id = blueprintLibrary.getId();
        }
        blueprintLibraryPO.setId(id);
        blueprintLibraryPO.setType(blueprintLibrary.getType().name());
        blueprintLibraryPO.setUrl(blueprintLibrary.getUrl());
        blueprintLibraryPO.setBranch(blueprintLibrary.getBranch());
        blueprintLibraryPO.setCurrentVersion(blueprintLibrary.getCurrentVersion());
        blueprintLibraryPO.setRemoteVersion(blueprintLibrary.getRemoteVersion());
        blueprintLibraryPO.setSourceType(blueprintLibrary.getSourceType().name());
        blueprintLibraryPO.setSyncStatus(blueprintLibrary.getSyncStatus());
        blueprintLibraryPO.setSyncedAt(blueprintLibrary.getSyncedAt());
        blueprintLibraryPO.setSyncMessage(blueprintLibrary.getSyncMessage());
        return blueprintLibraryPO;
    }

    public static class Constants {
        public static final String CACHE_NAME_LIBRARY = "blueprint-library:library";
    }
}