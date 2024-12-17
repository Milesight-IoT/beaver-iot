package com.milesight.beaveriot.entity.facade;

import com.milesight.beaveriot.entity.dto.EntityDTO;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/25 16:46
 */
public interface IEntityFacade {

    List<EntityDTO> getUserOrTargetEntities(Long userId, List<String> targetIds);

    List<EntityDTO> getTargetEntities(List<String> targetIds);

    void deleteCustomizedEntitiesByIds(List<Long> entityIds);

}
