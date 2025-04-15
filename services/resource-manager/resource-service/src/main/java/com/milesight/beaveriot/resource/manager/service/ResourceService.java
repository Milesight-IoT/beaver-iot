package com.milesight.beaveriot.resource.manager.service;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.resource.ResourceStorage;
import com.milesight.beaveriot.resource.manager.constants.ResourceManagerConstants;
import com.milesight.beaveriot.resource.manager.dto.ResourceDTO;
import com.milesight.beaveriot.resource.manager.dto.ResourcePersistDTO;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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

    public PreSignResult createPreSign(RequestUploadConfig request) {
        PreSignResult preSignResult = resourceStorage.createUploadPreSign(request.getFileName());
        ResourceTempPO resourceTempPO = new ResourceTempPO();
        resourceTempPO.setId(SnowflakeUtil.nextId());
        resourceTempPO.setCreatedAt(System.currentTimeMillis());
        resourceTempPO.setExpiredAt(resourceTempPO.getCreatedAt() + (ResourceManagerConstants.TEMP_RESOURCE_LIVE_MINUTES * 60 * 1000));

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
    public void persistByUrl(String url, ResourcePersistDTO resourcePersistDTO) {
        ResourcePO resourcePO = resourceRepository.findOneByUrl(url);
        if (resourcePO == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Resource url not found: " + url).build();
        }

        this.persist(resourcePO, resourcePersistDTO);
    }

    private void persist(ResourcePO resourcePO, ResourcePersistDTO resourcePersistDTO) {
        ResourceStat stat = resourceStorage.stat(resourcePO.getKey());
        if (stat == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Resource data storage not found!").build();
        }

        resourceTempRepository.deleteByResourceId(resourcePO.getId());

        resourcePO.setContentLength(stat.getSize());
        resourcePO.setContentType(stat.getContentType());
        resourcePO.setUpdatedBy(getCurrentUser());
        resourceRepository.save(resourcePO);
        ResourceRefPO resourceRefPO = new ResourceRefPO();
        resourceRefPO.setId(SnowflakeUtil.nextId());
        resourceRefPO.setResourceId(resourcePO.getId());
        resourceRefPO.setRefId(resourcePersistDTO.getRefId());
        resourceRefPO.setRefType(resourcePersistDTO.getRefType());
        resourceRefRepository.save(resourceRefPO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @DistributedLock(name = "RESOURCE_PERSIST(#{#p0})", scope = LockScope.GLOBAL)
    public void deleteByUrl(String url) {
        ResourcePO resourcePO = resourceRepository.findOneByUrl(url);
        if (resourcePO == null) {
            return;
        }

        resourceTempRepository.deleteByResourceId(resourcePO.getId());
        resourceRefRepository.deleteByResourceId(resourcePO.getId());
        try {
            resourceStorage.delete(resourcePO.getKey());
        } catch (Exception e) {
            log.error("Delete resource " + url + " error: " + e.getMessage());
        }

        resourceRepository.delete(resourcePO);
    }

    @Override
    public void unlinkRef(String refId, String refType) {
        resourceRefRepository.deleteByRefIdAndRefType(refId, refType);
    }

    // TODO: Potential storage leak: upload object by put after delete

    // TODO: schedule periodic deleting temp resource
}
