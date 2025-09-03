package com.milesight.beaveriot.dashboard.convert;

import com.milesight.beaveriot.canvas.model.dto.CanvasDTO;
import com.milesight.beaveriot.dashboard.dto.DashboardDTO;
import com.milesight.beaveriot.dashboard.model.response.DashboardCanvasItemResponse;
import com.milesight.beaveriot.dashboard.model.response.DashboardListItemResponse;
import com.milesight.beaveriot.dashboard.po.DashboardPO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/18 9:50
 */
@Mapper
public interface DashboardConvert {

    DashboardConvert INSTANCE = Mappers.getMapper(DashboardConvert.class);

    @Mapping(source = "id", target = "dashboardId")
    DashboardListItemResponse convertListItemResponse(DashboardPO dashboardPO);

    List<DashboardListItemResponse> convertResponseList(List<DashboardPO> dashboardPOList);

    @Mapping(source = "id", target = "dashboardId")
    @Mapping(source = "name", target = "dashboardName")
    DashboardDTO convertDTO(DashboardPO dashboardPO);

    List<DashboardDTO> convertDTOList(List<DashboardPO> dashboardPOList);

    @Mapping(source = "id", target = "canvasId")
    @Mapping(target = "id", ignore = true)
    DashboardCanvasItemResponse convertCanvasResponse(CanvasDTO canvasDTOList);

}
