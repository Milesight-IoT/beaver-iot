package com.milesight.beaveriot.resource.manager.facade;

import com.milesight.beaveriot.context.model.ResourceRefDTO;

import java.io.InputStream;

/**
 * ResourceManagerFacade class.
 *
 * @author simon
 * @date 2025/4/14
 */
public interface ResourceManagerFacade {
    void linkByUrl(String url, ResourceRefDTO resourceRefDTO);
    void unlinkRef(ResourceRefDTO resourceRefDTO);
    void unlinkRefAsync(ResourceRefDTO resourceRefDTO);
    String putTempResource(String fileName, String contentType, byte[] data);
    InputStream getDataByUrl(String url);
    String getResourceNameByUrl(String url);
}
