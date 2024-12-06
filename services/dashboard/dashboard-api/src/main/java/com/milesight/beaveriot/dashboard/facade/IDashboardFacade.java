package com.milesight.beaveriot.dashboard.facade;

import com.milesight.beaveriot.dashboard.dto.DashboardDTO;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/26 11:35
 */
public interface IDashboardFacade {

    List<DashboardDTO> getUserDashboards(Long userId);

    List<DashboardDTO> getDashboardsLike(String keyword);

    List<DashboardDTO> getDashboardsByIds(List<Long> dashboardIds);
}
