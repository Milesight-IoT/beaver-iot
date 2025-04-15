package com.milesight.beaveriot.resource.adapter.db.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.resource.adapter.db.service.model.DbResourceBasicProjection;
import com.milesight.beaveriot.resource.adapter.db.service.po.DbResourceDataPO;
import com.milesight.beaveriot.resource.adapter.db.service.po.DbResourceDataPreSignPO;
import com.milesight.beaveriot.resource.adapter.db.service.repository.DbResourceDataPreSignRepository;
import com.milesight.beaveriot.resource.adapter.db.service.repository.DbResourceDataRepository;
import com.milesight.beaveriot.resource.config.ResourceConstants;
import com.milesight.beaveriot.resource.model.ResourceStat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

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
    DbResourceDataPreSignRepository preSignRepository;

    public String preSign(String objKey) {
        DbResourceDataPreSignPO preSignPO = preSignRepository.findById(objKey).orElse(null);
        if (preSignPO == null) {
            preSignPO = new DbResourceDataPreSignPO();
            preSignPO.setObjKey(objKey);
        }

        preSignPO.setExpiredAt(System.currentTimeMillis() + ResourceConstants.PUT_RESOURCE_PRE_SIGN_EXPIRY_MINUTES * 60 * 1000);
        preSignRepository.save(preSignPO);
        return "/" + DbResourceConstants.RESOURCE_URL_PREFIX + "/" + objKey;
    }

    public boolean validateSign(String objKey) {
        DbResourceDataPreSignPO preSignPO = preSignRepository.findById(objKey).orElse(null);
        if (preSignPO == null) {
            return false;
        }

        return preSignPO.getExpiredAt() >= System.currentTimeMillis();
    }

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

    public DbResourceDataPO getResource(String objKey) {
        return resourceDataRepository.findByObjKey(objKey).orElse(null);
    }

    public void deleteResource(String objKey) {
        DbResourceDataPO resourceData = getResource(objKey);
        if (resourceData == null) {
            return;
        }

        resourceDataRepository.delete(resourceData);
    }

    // TODO: delete expired pre sign
}
