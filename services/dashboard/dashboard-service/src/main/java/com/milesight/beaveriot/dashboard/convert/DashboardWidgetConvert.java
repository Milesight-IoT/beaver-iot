package com.milesight.beaveriot.dashboard.convert;

import com.milesight.beaveriot.dashboard.model.dto.DashboardWidgetDTO;
import com.milesight.beaveriot.dashboard.po.DashboardWidgetPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/18 9:50
 */
@Mapper
public interface DashboardWidgetConvert {

    DashboardWidgetConvert INSTANCE = Mappers.getMapper(DashboardWidgetConvert.class);

    @Mapping(source = "id", target = "widgetId")
    DashboardWidgetDTO convertResponse(DashboardWidgetPO dashboardWidgetPO);

    List<DashboardWidgetDTO> convertResponseList(List<DashboardWidgetPO> dashboardWidgetPOList);

}
