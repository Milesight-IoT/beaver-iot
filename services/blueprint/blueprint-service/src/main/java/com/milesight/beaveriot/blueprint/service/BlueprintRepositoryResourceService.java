package com.milesight.beaveriot.blueprint.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.blueprint.model.BlueprintRepositoryResource;
import com.milesight.beaveriot.blueprint.po.BlueprintRepositoryResourcePO;
import com.milesight.beaveriot.blueprint.repository.BlueprintRepositoryResourceRepository;
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
public class BlueprintRepositoryResourceService {
    private static final int BLUEPRINT_REPOSITORY_RESOURCE_BATCH_SIZE = 100;
    private final BlueprintRepositoryResourceRepository blueprintRepositoryResourceRepository;

    public BlueprintRepositoryResourceService(BlueprintRepositoryResourceRepository blueprintRepositoryResourceRepository) {
        this.blueprintRepositoryResourceRepository = blueprintRepositoryResourceRepository;
    }

    @Transactional(rollbackFor = Throwable.class)
    public void batchSave(List<BlueprintRepositoryResource> blueprintRepositoryResources) {
        int batchSize = BLUEPRINT_REPOSITORY_RESOURCE_BATCH_SIZE;
        int parallelism = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(parallelism);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < blueprintRepositoryResources.size(); i += batchSize) {
            int toIndex = Math.min(i + batchSize, blueprintRepositoryResources.size());
            List<BlueprintRepositoryResource> subList = new ArrayList<>(blueprintRepositoryResources.subList(i, toIndex));
            CompletableFuture<Void> future = CompletableFuture.runAsync(() ->
                    blueprintRepositoryResourceRepository.saveAll(subList.stream().map(this::convertModelToPO).toList()), executor);
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

    public BlueprintRepositoryResource getResource(Long repositoryId, String repositoryVersion, String resourcePath) {
        BlueprintRepositoryResourcePO blueprintRepositoryResourcePO = blueprintRepositoryResourceRepository.findByRepositoryIdAndRepositoryVersionAndPath(repositoryId, repositoryVersion, resourcePath);
        if (blueprintRepositoryResourcePO == null) {
            return null;
        }

        return convertPOToModel(blueprintRepositoryResourcePO);
    }

    @Transactional(rollbackFor = Throwable.class)
    public void deleteAllByRepositoryIdAndRepositoryVersion(Long repositoryId, String repositoryVersion) {
        blueprintRepositoryResourceRepository.deleteAllByRepositoryIdAndRepositoryVersion(repositoryId, repositoryVersion);
    }

    public BlueprintRepositoryResourcePO convertModelToPO(BlueprintRepositoryResource blueprintRepositoryResource) {
        BlueprintRepositoryResourcePO blueprintRepositoryResourcePO = new BlueprintRepositoryResourcePO();
        long id;
        if (blueprintRepositoryResourcePO.getId() == null) {
            id = SnowflakeUtil.nextId();
        } else {
            id = blueprintRepositoryResourcePO.getId();
        }
        blueprintRepositoryResourcePO.setId(id);
        blueprintRepositoryResourcePO.setPath(blueprintRepositoryResource.getPath());
        blueprintRepositoryResourcePO.setContent(blueprintRepositoryResource.getContent());
        blueprintRepositoryResourcePO.setRepositoryId(blueprintRepositoryResource.getRepositoryId());
        blueprintRepositoryResourcePO.setRepositoryVersion(blueprintRepositoryResource.getRepositoryVersion());
        return blueprintRepositoryResourcePO;
    }

    public BlueprintRepositoryResource convertPOToModel(BlueprintRepositoryResourcePO blueprintRepositoryResourcePO) {
        return BlueprintRepositoryResource.builder()
                .path(blueprintRepositoryResourcePO.getPath())
                .content(blueprintRepositoryResourcePO.getContent())
                .repositoryId(blueprintRepositoryResourcePO.getRepositoryId())
                .repositoryVersion(blueprintRepositoryResourcePO.getRepositoryVersion())
                .build();
    }
}
