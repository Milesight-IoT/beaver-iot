package com.milesight.beaveriot.resource.manager.facade;

import com.milesight.beaveriot.resource.manager.dto.ResourceRefDTO;

/**
 * ResourceManagerFacade class.
 *
 * @author simon
 * @date 2025/4/14
 */
public interface ResourceManagerFacade {
    void linkByUrl(String url, ResourceRefDTO resourceRefDTO);

    void unlinkRef(ResourceRefDTO resourceRefDTO);
}
