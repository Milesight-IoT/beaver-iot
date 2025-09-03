package com.milesight.beaveriot.blueprint.service;

import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.blueprint.model.BlueprintRepository;
import com.milesight.beaveriot.blueprint.model.BlueprintRepositoryAddress;
import com.milesight.beaveriot.blueprint.po.BlueprintRepositoryPO;
import com.milesight.beaveriot.blueprint.repository.BlueprintRepositoryRepository;
import com.milesight.beaveriot.context.support.SpringContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * author: Luxb
 * create: 2025/9/1 9:41
 **/
@Slf4j
@Service
public class BlueprintRepositoryService {
    private final BlueprintRepositoryRepository blueprintRepositoryRepository;
    private final BlueprintRepositoryAddressService blueprintRepositoryAddressService;

    public BlueprintRepositoryService(BlueprintRepositoryRepository blueprintRepositoryRepository, BlueprintRepositoryAddressService blueprintRepositoryAddressService) {
        this.blueprintRepositoryRepository = blueprintRepositoryRepository;
        this.blueprintRepositoryAddressService = blueprintRepositoryAddressService;
    }

    @Cacheable(cacheNames = Constants.CACHE_NAME_REPOSITORY, key = "#p0 + '@' + #p1", unless = "#result == null")
    public BlueprintRepository getBlueprintRepository(String home, String branch) {
        if (StringUtils.isEmpty(home) || StringUtils.isEmpty(branch)) {
            return null;
        }

        BlueprintRepositoryPO blueprintRepositoryPO = blueprintRepositoryRepository.findByHomeAndBranch(home, branch);
        if (blueprintRepositoryPO == null) {
            return null;
        }

        return convertPOToModel(blueprintRepositoryPO);
    }

    @SuppressWarnings("unused")
    @CacheEvict(cacheNames = Constants.CACHE_NAME_REPOSITORY, key = "#p0 + '@' + #p1")
    public void evictCacheBlueprintRepository(String home, String branch) {
        log.debug("Evict cache: {}, key: {}@{}",
                Constants.CACHE_NAME_REPOSITORY,
                home,
                branch);
    }

    public BlueprintRepository getCurrentBlueprintRepository() {
        BlueprintRepositoryAddress blueprintRepositoryAddress = blueprintRepositoryAddressService.getCurrentBlueprintRepositoryAddress();
        return self().getBlueprintRepository(blueprintRepositoryAddress.getHome(), blueprintRepositoryAddress.getBranch());
    }

    public BlueprintRepositoryService self() {
        return SpringContext.getBean(BlueprintRepositoryService.class);
    }

    public void save(BlueprintRepository blueprintRepository) {
        BlueprintRepositoryPO blueprintRepositoryPO = convertModelToPO(blueprintRepository);
        blueprintRepositoryRepository.save(blueprintRepositoryPO);
        blueprintRepository.setId(blueprintRepositoryPO.getId());
    }

    public BlueprintRepository convertPOToModel(BlueprintRepositoryPO blueprintRepositoryPO) {
        return BlueprintRepository.builder()
                .id(blueprintRepositoryPO.getId())
                .home(blueprintRepositoryPO.getHome())
                .branch(blueprintRepositoryPO.getBranch())
                .currentVersion(blueprintRepositoryPO.getCurrentVersion())
                .remoteVersion(blueprintRepositoryPO.getRemoteVersion())
                .syncStatus(blueprintRepositoryPO.getSyncStatus())
                .syncedAt(blueprintRepositoryPO.getSyncedAt()).build();
    }

    public BlueprintRepositoryPO convertModelToPO(BlueprintRepository blueprintRepository) {
        BlueprintRepositoryPO blueprintRepositoryPO = new BlueprintRepositoryPO();
        long id;
        if (blueprintRepository.getId() == null) {
            id = SnowflakeUtil.nextId();
        } else {
            id = blueprintRepository.getId();
        }
        blueprintRepositoryPO.setId(id);
        blueprintRepositoryPO.setHome(blueprintRepository.getHome());
        blueprintRepositoryPO.setBranch(blueprintRepository.getBranch());
        blueprintRepositoryPO.setCurrentVersion(blueprintRepository.getCurrentVersion());
        blueprintRepositoryPO.setRemoteVersion(blueprintRepository.getRemoteVersion());
        blueprintRepositoryPO.setSyncStatus(blueprintRepository.getSyncStatus());
        blueprintRepositoryPO.setSyncedAt(blueprintRepository.getSyncedAt());
        return blueprintRepositoryPO;
    }

    public static class Constants {
        public static final String CACHE_NAME_REPOSITORY = "blueprint-repository:repository";
    }
}