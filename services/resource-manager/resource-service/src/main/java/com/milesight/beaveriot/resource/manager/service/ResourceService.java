package com.milesight.beaveriot.resource.manager.service;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.resource.ResourceStorage;
import com.milesight.beaveriot.resource.config.ResourceConstants;
import com.milesight.beaveriot.resource.manager.constants.ResourceManagerConstants;
import com.milesight.beaveriot.resource.manager.dto.ResourceRefDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ResourceService class.
 *
 * @author simon
 * @date 2025/4/14
 */
@Service
@Slf4j
public class ResourceService implements ResourceManagerFacade {
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

    public PreSignResult createPreSign(RequestUploadConfig request) {
        PreSignResult preSignResult = resourceStorage.createUploadPreSign(request.getFileName());
        ResourceTempPO resourceTempPO = new ResourceTempPO();
        resourceTempPO.setId(SnowflakeUtil.nextId());
        resourceTempPO.setCreatedAt(System.currentTimeMillis());
        resourceTempPO.setExpiredAt(resourceTempPO.getCreatedAt() + (ResourceManagerConstants.TEMP_RESOURCE_LIVE_MINUTES * 60 * 1000));
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
        ScheduleRule rule = new ScheduleRule();
        rule.setPeriodSecond(ResourceManagerConstants.CLEAR_TEMP_RESOURCE_INTERVAL.toSeconds());
        ScheduleSettings settings = new ScheduleSettings();
        settings.setScheduleType(ScheduleType.FIXED_RATE);
        settings.setScheduleRule(rule);
        scheduler.schedule("clear-temp-resource", settings, task -> this.clearExpiredTempResource());
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
}
