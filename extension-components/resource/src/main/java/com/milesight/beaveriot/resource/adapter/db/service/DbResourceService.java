package com.milesight.beaveriot.resource.adapter.db.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.constants.CacheKeyConstants;
import com.milesight.beaveriot.resource.adapter.db.service.model.DbResourceBasicProjection;
import com.milesight.beaveriot.resource.adapter.db.service.po.DbResourceDataPO;
import com.milesight.beaveriot.resource.adapter.db.service.model.DbResourceDataPreSignData;
import com.milesight.beaveriot.resource.adapter.db.service.repository.DbResourceDataRepository;
import com.milesight.beaveriot.resource.config.ResourceConstants;
import com.milesight.beaveriot.resource.config.ResourceSettings;
import com.milesight.beaveriot.resource.model.ResourceStat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DbResourceService class.
 *
 * @author simon
 * @date 2025/4/7
 */
@Service
public class DbResourceService {
    @Autowired
    DbResourceDataRepository resourceDataRepository;

    @Autowired
    ResourceSettings resourceSettings;

    @Autowired
    CacheManager cacheManager;

    public DbResourceDataPreSignData getPreSignData(String objKey) {
        Cache cache = cacheManager.getCache(CacheKeyConstants.PRE_SIGN_CACHE_NAME);
        assert cache != null;
        return cache.get(objKey, DbResourceDataPreSignData.class);
    }

    public void putPreSignData(String objKey, DbResourceDataPreSignData data) {
        Cache cache = cacheManager.getCache(CacheKeyConstants.PRE_SIGN_CACHE_NAME);
        assert cache != null;
        cache.put(objKey, data);
    }

    public String preSign(String objKey) {
        DbResourceDataPreSignData preSignData = getPreSignData(objKey);
        if (preSignData == null) {
            preSignData = new DbResourceDataPreSignData();
            preSignData.setObjKey(objKey);
        }

        preSignData.setExpiredAt(System.currentTimeMillis() + resourceSettings.getPreSignExpire().toMillis());
        putPreSignData(objKey, preSignData);
        return "/" + DbResourceConstants.RESOURCE_URL_PREFIX + "/" + objKey;
    }

    public boolean validateSign(String objKey) {
        DbResourceDataPreSignData preSignPO = getPreSignData(objKey);
        if (preSignPO == null) {
            return false;
        }

        return preSignPO.getExpiredAt() >= System.currentTimeMillis();
    }

    @CacheEvict(cacheNames = CacheKeyConstants.RESOURCE_DATA_CACHE_NAME, key = "#p0")
    public void putResource(String objKey, String contentType, byte[] data) {
        if (!validateSign(objKey)) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED)
                    .detailMessage("Invalid pre sign.")
                    .build();
        }

        DbResourceDataPO resourceDataPO = resourceDataRepository.findByObjKey(objKey).orElse(null);
        if (resourceDataPO == null) {
            resourceDataPO = new DbResourceDataPO();
            resourceDataPO.setId(SnowflakeUtil.nextId());
            resourceDataPO.setObjKey(objKey);
        }

        if (!StringUtils.hasText(contentType)) {
            resourceDataPO.setContentType(DbResourceConstants.RESOURCE_DEFAULT_CONTENT_TYPE);
        } else {
            resourceDataPO.setContentType(contentType);
        }

        resourceDataPO.setContentLength((long) data.length);
        resourceDataPO.setData(data);
        resourceDataRepository.save(resourceDataPO);
    }

    public ResourceStat statResource(String objKey) {
        List<DbResourceBasicProjection> infoList = resourceDataRepository.findBasicByKeys(List.of(objKey));
        if (infoList.size() != 1) {
            return null;
        }

        DbResourceBasicProjection basicInfo = infoList.get(0);
        ResourceStat stat = new ResourceStat();
        stat.setSize(basicInfo.getContentLength());
        stat.setContentType(basicInfo.getContentType());
        return stat;
    }

    @Cacheable(cacheNames = CacheKeyConstants.RESOURCE_DATA_CACHE_NAME, key = "#p0")
    public DbResourceDataPO getResource(String objKey) {
        return resourceDataRepository.findByObjKey(objKey).orElse(null);
    }

    public void deleteResource(String objKey) {
        DbResourceDataPO resourceData = getResource(objKey);
        if (resourceData == null) {
            return;
        }

        Cache cache = cacheManager.getCache(CacheKeyConstants.PRE_SIGN_CACHE_NAME);
        assert cache != null;
        cache.evict(objKey);

        resourceDataRepository.delete(resourceData);
    }
}
