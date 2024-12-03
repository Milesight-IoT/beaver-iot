package com.milesight.beaveriot.dashboard.convert;

import com.milesight.beaveriot.dashboard.model.response.DashboardResponse;
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
    DashboardResponse convertResponse(DashboardPO dashboardPO);

    List<DashboardResponse> convertResponseList(List<DashboardPO> dashboardPOList);

}
