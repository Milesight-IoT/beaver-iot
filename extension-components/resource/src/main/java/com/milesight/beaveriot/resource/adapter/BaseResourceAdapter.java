package com.milesight.beaveriot.resource.adapter;

import com.milesight.beaveriot.resource.model.PutResourceRequest;
import com.milesight.beaveriot.resource.model.ResourceStat;

import java.net.URL;

/**
 * BaseResourceAdapter
 *
 * @author simon
 * @date 2025/4/2
 */
public interface BaseResourceAdapter {
    String generatePutResourcePreSign(String objKey);

    String resolveResourceUrl(String objKey);

    ResourceStat stat(String objKey);

    byte[] get(String objKey);

    void delete(String objKey);

    void putResource(PutResourceRequest request);
}
