package com.milesight.beaveriot.user.convert;

import com.milesight.beaveriot.user.model.Menu;
import com.milesight.beaveriot.user.model.response.MenuResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.factory.Mappers;

/**
 * @author loong
 * @date 2024/11/22 10:47
 */
@Mapper
public interface MenuConverter {

    MenuConverter INSTANCE = Mappers.getMapper(MenuConverter.class);

    @Mappings({
            @Mapping(source = "id", target = "menuId"),
    })
    MenuResponse convertResponse(Menu menu);

}
