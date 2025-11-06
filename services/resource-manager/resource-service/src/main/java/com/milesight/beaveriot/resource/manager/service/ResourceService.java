package com.milesight.beaveriot.resource.manager.service;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.ResourceServiceProvider;
import com.milesight.beaveriot.context.enums.ResourceRefType;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.data.model.TimeSeriesCategory;
import com.milesight.beaveriot.data.timeseries.common.TimeSeriesProperty;
import com.milesight.beaveriot.resource.ResourceStorage;
import com.milesight.beaveriot.resource.manager.constants.ResourceManagerConstants;
import com.milesight.beaveriot.context.model.ResourceRefDTO;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import com.milesight.beaveriot.resource.manager.model.request.RequestUploadConfig;
import com.milesight.beaveriot.resource.manager.po.ResourcePO;
import com.milesight.beaveriot.resource.manager.po.ResourceRefPO;
import com.milesight.beaveriot.resource.manager.po.ResourceTempPO;
import com.milesight.beaveriot.resource.manager.repository.ResourceRefRepository;
import com.milesight.beaveriot.resource.manager.repository.ResourceRepository;
import com.milesight.beaveriot.resource.manager.repository.ResourceTempRepository;
import com.milesight.beaveriot.resource.model.PreSignResult;
import com.milesight.beaveriot.resource.model.ResourceStat;
import com.milesight.beaveriot.scheduler.core.Scheduler;
import com.milesight.beaveriot.scheduler.core.model.ScheduleRule;
import com.milesight.beaveriot.scheduler.core.model.ScheduleSettings;
import com.milesight.beaveriot.scheduler.core.model.ScheduleType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.io.InputStream;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

/**
 * ResourceService class.
 *
 * @author simon
 * @date 2025/4/14
 */
@Service
@Slf4j
public class ResourceService implements ResourceManagerFacade, ResourceServiceProvider {
    ThreadPoolExecutor asyncUnlinkThreadPoolExecutor;

    @Autowired
    ResourceStorage resourceStorage;

    @Autowired
    ResourceRepository resourceRepository;

    @Autowired
    ResourceTempRepository resourceTempRepository;

    @Autowired
    ResourceRefRepository resourceRefRepository;

    @Autowired
    Scheduler scheduler;

    @Autowired
    TimeSeriesProperty timeSeriesProperty;

    private ThreadPoolExecutor buildAsyncUnlinkThreadPoolExecutor() {
        int corePoolSize = Runtime.getRuntime().availableProcessors() * 2;
        int maxPoolSize = corePoolSize * 2;
        long keepAliveTime = 60L;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>(1000);
        RejectedExecutionHandler handler = new ThreadPoolExecutor.CallerRunsPolicy();

        return new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                handler
        );
    }

    @Override
    public String putTempResource(String fileName, String contentType, byte[] data) {
        RequestUploadConfig request = new RequestUploadConfig();
        request.setFileName(fileName);

        PreSignResult preSignResult = createPreSign(request);
        resourceStorage.upload(preSignResult.getKey(), contentType, data);
        return preSignResult.getResourceUrl();
    }

    public PreSignResult createPreSign(RequestUploadConfig request) {
        PreSignResult preSignResult = resourceStorage.createUploadPreSign(request.getFileName());
        ResourceTempPO resourceTempPO = new ResourceTempPO();
        resourceTempPO.setId(SnowflakeUtil.nextId());
        resourceTempPO.setCreatedAt(System.currentTimeMillis());

        resourceTempPO.setExpiredAt(resourceTempPO.getCreatedAt() + (getTempResourceLiveMinutes(request.getTempResourceLiveMinutes()) * 60 * 1000));
        resourceTempPO.setSettled(false);

        ResourcePO resourcePO = new ResourcePO();
        resourcePO.setId(SnowflakeUtil.nextId());

        resourceTempPO.setResourceId(resourcePO.getId());
        resourceTempRepository.save(resourceTempPO);

        resourcePO.setKey(preSignResult.getKey());
        resourcePO.setUrl(preSignResult.getResourceUrl());
        resourcePO.setName(Optional.ofNullable(request.getName()).orElse(request.getFileName()));
        resourcePO.setDescription(request.getDescription());
        String createdBy = getCurrentUser();
        resourcePO.setCreatedBy(createdBy);
        resourcePO.setUpdatedBy(createdBy);
        resourceRepository.save(resourcePO);

        return preSignResult;
    }

    private long getTempResourceLiveMinutes(Integer tempResourceLiveMinutes) {
        if (tempResourceLiveMinutes == null) {
            return ResourceManagerConstants.TEMP_RESOURCE_LIVE_MINUTES;
        } else if (tempResourceLiveMinutes > ResourceManagerConstants.MAX_TEMP_RESOURCE_LIVE_MINUTES) {
            return ResourceManagerConstants.MAX_TEMP_RESOURCE_LIVE_MINUTES;
        } else {
            return tempResourceLiveMinutes;
        }
    }

    private String getCurrentUser() {
        return SecurityUserContext.getUserId() == null ? null : SecurityUserContext.getUserId().toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(name = "RESOURCE_PERSIST(#{#p0})", scope = LockScope.GLOBAL)
    public void linkByUrl(String url, ResourceRefDTO resourceRefDTO) {
        ResourcePO resourcePO = resourceRepository.findOneByUrl(url);
        if (resourcePO == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Resource url not found: " + url).build();
        }

        this.link(resourcePO, resourceRefDTO);
    }

    private void link(ResourcePO resourcePO, ResourceRefDTO resourceRefDTO) {
        if (resourcePO.getContentLength() == null) {
            ResourceStat stat = resourceStorage.stat(resourcePO.getKey());
            if (stat == null) {
                throw ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Resource data storage not found!").build();
            }

            Optional<ResourceTempPO> tempPO = resourceTempRepository.findOne(f -> f.eq(ResourceTempPO.Fields.resourceId, resourcePO.getId()));
            if (tempPO.isPresent() && tempPO.get().getSettled().equals(Boolean.FALSE)) {
                tempPO.get().setSettled(true);
                resourceTempRepository.save(tempPO.get());
            }

            resourcePO.setContentLength(stat.getSize());
            resourcePO.setContentType(stat.getContentType());
            resourcePO.setUpdatedBy(getCurrentUser());
            resourceRepository.save(resourcePO);
        }

        if (resourceRefRepository.count(f -> f
                .eq(ResourceRefPO.Fields.resourceId, resourcePO.getId())
                .eq(ResourceRefPO.Fields.refId, resourceRefDTO.getRefId())
                .eq(ResourceRefPO.Fields.refType, resourceRefDTO.getRefType())
            ) > 0
        ) {
            return;
        }

        ResourceRefPO resourceRefPO = new ResourceRefPO();
        resourceRefPO.setId(SnowflakeUtil.nextId());
        resourceRefPO.setResourceId(resourcePO.getId());
        resourceRefPO.setRefId(resourceRefDTO.getRefId());
        resourceRefPO.setRefType(resourceRefDTO.getRefType());
        resourceRefRepository.save(resourceRefPO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlinkRef(ResourceRefDTO resourceRefDTO) {
        List<ResourceRefPO> refList = resourceRefRepository.findByRefIdAndRefType(resourceRefDTO.getRefId(), resourceRefDTO.getRefType());
        if (refList.isEmpty()) {
            return;
        }

        resourceRefRepository.deleteAll(refList);
        cleanUpResources(refList.stream().map(ResourceRefPO::getResourceId).distinct().toList());
    }

    public void unlinkRefAsync(ResourceRefDTO resourceRefDTO) {
        ResourceService self = self();
        String tenantId = TenantContext.getTenantId();
        asyncUnlinkThreadPoolExecutor.execute(() -> {
            TenantContext.setTenantId(tenantId);
            self.unlinkRef(resourceRefDTO);
        });
    }

    public ResourceService self() {
        return (ResourceService) AopContext.currentProxy();
    }

    @Override
    public InputStream getDataByUrl(String url) {
        ResourcePO resourcePO = resourceRepository.findOneByUrl(url);
        if (resourcePO == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Resource url not found: " + url).build();
        }

        return resourceStorage.get(resourcePO.getKey());
    }

    @Override
    public String getResourceNameByUrl(String url) {
        ResourcePO resourcePO = resourceRepository.findOneByUrl(url);
        if (resourcePO == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Resource url not found: " + url).build();
        }

        return resourcePO.getName();
    }

    /**
     * Detect the resources and remove those that have no links.
     *
     * @param resourceIdList resources to detect
     */
    private void cleanUpResources(List<Long> resourceIdList) {
        Set<Long> resourceIdToRemove = new HashSet<>(resourceIdList);
        resourceRefRepository.findByResourceIdIn(resourceIdList).forEach(r -> resourceIdToRemove.remove(r.getResourceId()));
        if (resourceIdToRemove.isEmpty()) {
            return;
        }

        List<ResourcePO> resourceToRemove = resourceRepository.findAllById(resourceIdToRemove);
        resourceRepository.deleteAll(resourceToRemove);
        resourceToRemove.forEach(r -> resourceStorage.delete(r.getKey()));
    }

    @PostConstruct
    protected void init() {
        asyncUnlinkThreadPoolExecutor = buildAsyncUnlinkThreadPoolExecutor();

        ScheduleRule rule = new ScheduleRule();
        rule.setPeriodSecond(ResourceManagerConstants.CLEAR_TEMP_RESOURCE_INTERVAL.toSeconds());
        ScheduleSettings settings = new ScheduleSettings();
        settings.setScheduleType(ScheduleType.FIXED_RATE);
        settings.setScheduleRule(rule);
        scheduler.schedule("clear-temp-resource", settings, task -> this.clearExpiredTempResource());

        rule = new ScheduleRule();
        rule.setPeriodSecond(ResourceManagerConstants.AUTO_UNLINK_RESOURCE_INTERVAL.toSeconds());
        settings = new ScheduleSettings();
        settings.setScheduleType(ScheduleType.FIXED_RATE);
        settings.setScheduleRule(rule);
        scheduler.schedule("auto-unlink-resource", settings, task -> this.autoUnlinkResource());
    }

    @PreDestroy
    public void onDestroy() {
        closeAsyncUnlinkThreadPoolExecutorGracefully();
    }

    private void closeAsyncUnlinkThreadPoolExecutorGracefully() {
        asyncUnlinkThreadPoolExecutor.shutdown();
        try {
            if (!asyncUnlinkThreadPoolExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                asyncUnlinkThreadPoolExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            asyncUnlinkThreadPoolExecutor.shutdownNow();
        }
    }

    protected Page<ResourceTempPO> getBatchExpiredTempResource() {
        return resourceTempRepository
                .findAll(f -> f.lt(ResourceTempPO.Fields.expiredAt, System.currentTimeMillis()), Pageable.ofSize(ResourceManagerConstants.CLEAR_TEMP_BATCH_SIZE));
    }

    protected void clearExpiredTempResource() {
        Page<ResourceTempPO> expiredTempList = getBatchExpiredTempResource();
        while (!expiredTempList.isEmpty()) {
            List<Long> resourceIdToClean = expiredTempList
                    .stream()
                    .filter(f -> f.getSettled().equals(Boolean.FALSE))
                    .map(ResourceTempPO::getResourceId).toList();
            List<ResourcePO> resourceToRemove = resourceRepository.findAllByIdIgnoreTenant(resourceIdToClean);
            resourceRepository.deleteAllIgnoreTenant(resourceToRemove);
            resourceToRemove.forEach(r -> resourceStorage.delete(r.getKey()));
            resourceTempRepository.deleteAll(expiredTempList);
            log.debug("Delete temp resources: {}", resourceIdToClean);

            expiredTempList = getBatchExpiredTempResource();
        }
    }

    protected void autoUnlinkResource() {
        unlinkRefType(ResourceRefType.ENTITY_HISTORY.name(), timeSeriesProperty.getRetention().get(TimeSeriesCategory.TELEMETRY));
    }

    private void unlinkRefType(String refType, Duration expireDuration) {
        Long expirationThreshold = System.currentTimeMillis() - expireDuration.toMillis();
        List<ResourceRefPO> pos = resourceRefRepository.findAll(f -> f.eq(ResourceRefPO.Fields.refType, refType)
                .and(f2 -> f2.lt(ResourceRefPO.Fields.createdAt, expirationThreshold)));
        if (CollectionUtils.isEmpty(pos)) {
            return;
        }

        log.debug("Unlinking {} resource references of type '{}' created before {}", pos.size(), refType, expirationThreshold);
        pos.forEach(po -> unlinkRefAsync(ResourceRefDTO.of(po.getRefId(), po.getRefType())));
    }
}
