package com.milesight.beaveriot.entity.facade;

import com.milesight.beaveriot.entity.dto.EntityDTO;
import com.milesight.beaveriot.entity.dto.EntityIdKeyDTO;
import com.milesight.beaveriot.entity.dto.EntityQuery;
import com.milesight.beaveriot.entity.dto.EntityResponse;
import org.springframework.data.domain.Page;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * @author loong
 * @date 2024/11/25 16:46
 */
public interface IEntityFacade {

    Page<EntityResponse> search(EntityQuery entityQuery);

    List<EntityDTO> getUserOrTargetEntities(Long userId, List<String> targetIds);

    List<EntityDTO> getTargetEntities(List<String> targetIds);

    Map<Long, String> mapEntityIdToAttachTargetId(Collection<Long> entityIds);

    void deleteEntitiesByIds(List<Long> entityIds);

    long countAllEntitiesByIntegrationId(String integrationId);

    Map<String, Long> countAllEntitiesByIntegrationIds(List<String> integrationIds);

    List<EntityIdKeyDTO> findIdAndKeyByIds(List<Long> entityIds);
}
