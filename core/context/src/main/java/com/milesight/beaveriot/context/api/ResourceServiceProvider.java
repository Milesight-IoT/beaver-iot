package com.milesight.beaveriot.context.api;

import com.milesight.beaveriot.context.model.ResourceRefDTO;

/**
 * author: Luxb
 * create: 2025/11/6 15:44
 **/
public interface ResourceServiceProvider {
    void linkByUrl(String url, ResourceRefDTO resourceRefDTO);
    void unlinkRef(ResourceRefDTO resourceRefDTO);
    String upload(String fileName, String contentType, byte[] data);
}
