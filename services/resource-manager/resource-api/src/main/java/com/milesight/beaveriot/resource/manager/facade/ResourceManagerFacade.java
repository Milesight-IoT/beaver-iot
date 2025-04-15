package com.milesight.beaveriot.resource.manager.facade;

import com.milesight.beaveriot.resource.manager.dto.ResourceDTO;
import com.milesight.beaveriot.resource.manager.dto.ResourcePersistDTO;

/**
 * ResourceManagerFacade class.
 *
 * @author simon
 * @date 2025/4/14
 */
public interface ResourceManagerFacade {
    void persistByUrl(String url, ResourcePersistDTO resourcePersistDTO);

    void deleteByUrl(String url);
}
