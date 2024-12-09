package com.milesight.beaveriot.entity.facade;

import com.milesight.beaveriot.entity.convert.EntityConverter;
import com.milesight.beaveriot.entity.dto.EntityDTO;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.entity.repository.EntityRepository;
import com.milesight.beaveriot.entity.service.EntityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/25 16:46
 */
@Service
public class EntityFacade implements IEntityFacade {

    @Autowired
    private EntityRepository entityRepository;

    @Autowired
    private EntityService entityService;

    public List<EntityDTO> getUserOrTargetEntities(Long userId, List<String> targetIds) {
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.or(filter1 -> filter1.eq(EntityPO.Fields.userId, userId)
                        .in(!targetIds.isEmpty(), EntityPO.Fields.attachTargetId, targetIds.stream().map(Long::parseLong).toArray())
                ));
        return EntityConverter.INSTANCE.convertDTOList(entityPOList);
    }

    public List<EntityDTO> getTargetEntities(List<String> targetIds) {
        List<EntityPO> entityPOList = entityRepository.findAll(filter -> filter.in(!targetIds.isEmpty(), EntityPO.Fields.attachTargetId, targetIds.stream().map(Long::parseLong).toArray())
        );
        return EntityConverter.INSTANCE.convertDTOList(entityPOList);
    }

    /**
     * Batch delete customized entities by ids
     *
     * @param entityIds entity ids
     */
    @Override
    public void deleteCustomizedEntitiesByIds(List<Long> entityIds) {
        entityService.deleteCustomizedEntitiesByIds(entityIds);
    }

}
