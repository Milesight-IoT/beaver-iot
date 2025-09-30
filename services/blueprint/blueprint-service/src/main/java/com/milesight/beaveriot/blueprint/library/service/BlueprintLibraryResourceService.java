package com.milesight.beaveriot.blueprint.library.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.blueprint.library.model.BlueprintLibraryResource;
import com.milesight.beaveriot.blueprint.library.po.BlueprintLibraryResourcePO;
import com.milesight.beaveriot.blueprint.library.repository.BlueprintLibraryResourceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * author: Luxb
 * create: 2025/9/1 17:17
 **/
@Service
public class BlueprintLibraryResourceService {
    private static final int BLUEPRINT_LIBRARY_RESOURCE_BATCH_SIZE = 100;
    private final BlueprintLibraryResourceRepository blueprintLibraryResourceRepository;

    public BlueprintLibraryResourceService(BlueprintLibraryResourceRepository blueprintLibraryResourceRepository) {
        this.blueprintLibraryResourceRepository = blueprintLibraryResourceRepository;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void batchSave(List<BlueprintLibraryResource> blueprintLibraryResources) {
        int batchSize = BLUEPRINT_LIBRARY_RESOURCE_BATCH_SIZE;
        int parallelism = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(parallelism);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < blueprintLibraryResources.size(); i += batchSize) {
            int toIndex = Math.min(i + batchSize, blueprintLibraryResources.size());
            List<BlueprintLibraryResource> subList = new ArrayList<>(blueprintLibraryResources.subList(i, toIndex));
            CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                    blueprintLibraryResourceRepository.saveAll(subList.stream().map(this::convertModelToPO).toList()), executor);
            futures.add(future);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    public BlueprintLibraryResource getResource(Long libraryId, String libraryVersion, String resourcePath) {
        BlueprintLibraryResourcePO blueprintLibraryResourcePO = blueprintLibraryResourceRepository.findByLibraryIdAndLibraryVersionAndPath(libraryId, libraryVersion, resourcePath);
        if (blueprintLibraryResourcePO == null) {
            return null;
        }

        return convertPOToModel(blueprintLibraryResourcePO);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAllByLibraryIdAndLibraryVersion(Long libraryId, String libraryVersion) {
        blueprintLibraryResourceRepository.deleteAllByLibraryIdAndLibraryVersion(libraryId, libraryVersion);
    }

    public BlueprintLibraryResourcePO convertModelToPO(BlueprintLibraryResource blueprintLibraryResource) {
        BlueprintLibraryResourcePO blueprintLibraryResourcePO = new BlueprintLibraryResourcePO();
        long id;
        if (blueprintLibraryResourcePO.getId() == null) {
            id = SnowflakeUtil.nextId();
        } else {
            id = blueprintLibraryResourcePO.getId();
        }
        blueprintLibraryResourcePO.setId(id);
        blueprintLibraryResourcePO.setPath(blueprintLibraryResource.getPath());
        blueprintLibraryResourcePO.setContent(blueprintLibraryResource.getContent());
        blueprintLibraryResourcePO.setLibraryId(blueprintLibraryResource.getLibraryId());
        blueprintLibraryResourcePO.setLibraryVersion(blueprintLibraryResource.getLibraryVersion());
        return blueprintLibraryResourcePO;
    }

    public BlueprintLibraryResource convertPOToModel(BlueprintLibraryResourcePO blueprintLibraryResourcePO) {
        return BlueprintLibraryResource.builder()
                .path(blueprintLibraryResourcePO.getPath())
                .content(blueprintLibraryResourcePO.getContent())
                .libraryId(blueprintLibraryResourcePO.getLibraryId())
                .libraryVersion(blueprintLibraryResourcePO.getLibraryVersion())
                .build();
    }
}
