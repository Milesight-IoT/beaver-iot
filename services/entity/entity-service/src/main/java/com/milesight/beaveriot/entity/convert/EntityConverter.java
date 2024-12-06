package com.milesight.beaveriot.entity.convert;

import com.milesight.beaveriot.entity.dto.EntityDTO;
import com.milesight.beaveriot.entity.po.EntityPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/22 10:47
 */
@Mapper
public interface EntityConverter {

    EntityConverter INSTANCE = Mappers.getMapper(EntityConverter.class);

    @Mappings({
            @Mapping(source = "id", target = "entityId"),
    })
    EntityDTO convertDTO(EntityPO entityPO);

    List<EntityDTO> convertDTOList(List<EntityPO> entityPOList);

}
