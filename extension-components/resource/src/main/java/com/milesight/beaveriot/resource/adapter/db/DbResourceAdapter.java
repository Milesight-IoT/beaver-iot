package com.milesight.beaveriot.resource.adapter.db;

import com.milesight.beaveriot.resource.adapter.BaseResourceAdapter;
import com.milesight.beaveriot.resource.adapter.db.service.DbResourceConstants;
import com.milesight.beaveriot.resource.adapter.db.service.DbResourceService;
import com.milesight.beaveriot.resource.model.PutResourceRequest;
import com.milesight.beaveriot.resource.model.ResourceStat;
import lombok.SneakyThrows;

import java.net.URI;
import java.net.URL;

/**
 * DbResourceAdapater class.
 *
 * @author simon
 * @date 2025/4/2
 */
public class DbResourceAdapter implements BaseResourceAdapter {
    private final DbResourceService resourceService;

    public DbResourceAdapter(DbResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @Override
    public String generatePutResourcePreSign(String objKey) {
        return resourceService.preSign(objKey);
    }

    @Override
    public ResourceStat stat(String objKey) {
        return resourceService.statResource(objKey);
    }

    @Override
    public byte[] get(String objKey) {
        return resourceService.getResource(objKey).getData();
    }

    @Override
    public void delete(String objKey) {
        resourceService.deleteResource(objKey);
    }

    @Override
    @SneakyThrows
    public void putResource(PutResourceRequest request) {
        resourceService.putResource(request.getObjectKey(), request.getContentType(), request.getContentInput().readAllBytes());
    }

    @Override
    public String resolveResourceUrl(String objKey) {
        return "/" + DbResourceConstants.RESOURCE_URL_PREFIX + "/" + objKey;
    }
}
