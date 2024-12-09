package com.milesight.beaveriot.dashboard.facade;

import com.milesight.beaveriot.dashboard.convert.DashboardConvert;
import com.milesight.beaveriot.dashboard.dto.DashboardDTO;
import com.milesight.beaveriot.dashboard.po.DashboardPO;
import com.milesight.beaveriot.dashboard.repository.DashboardRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @author loong
 * @date 2024/11/26 11:40
 */
@Service
public class DashboardFacade implements IDashboardFacade {

    @Autowired
    DashboardRepository dashboardRepository;

    @Override
    public List<DashboardDTO> getUserDashboards(Long userId) {
        List<DashboardPO> dashboardPOS = dashboardRepository.findAll(filterable -> filterable.eq(DashboardPO.Fields.userId, userId)
        );
        return DashboardConvert.INSTANCE.convertDTOList(dashboardPOS);
    }

    @Override
    public List<DashboardDTO> getDashboardsLike(String keyword) {
        List<DashboardPO> dashboardPOS = dashboardRepository.findAll(filterable -> filterable.like(StringUtils.hasText(keyword), DashboardPO.Fields.name, keyword)
        );
        return DashboardConvert.INSTANCE.convertDTOList(dashboardPOS);
    }

    @Override
    public List<DashboardDTO> getDashboardsByIds(List<Long> dashboardIds) {
        List<DashboardPO> dashboardPOS = dashboardRepository.findAll(filterable -> filterable.in(DashboardPO.Fields.id, dashboardIds.toArray())
        );
        return DashboardConvert.INSTANCE.convertDTOList(dashboardPOS);
    }

}
