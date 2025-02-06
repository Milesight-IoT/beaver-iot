package com.milesight.beaveriot.dashboard.facade;

import com.milesight.beaveriot.dashboard.convert.DashboardConvert;
import com.milesight.beaveriot.dashboard.dto.DashboardDTO;
import com.milesight.beaveriot.dashboard.po.DashboardPO;
import com.milesight.beaveriot.dashboard.repository.DashboardRepository;
import com.milesight.beaveriot.user.dto.UserDTO;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author loong
 * @date 2024/11/26 11:40
 */
@Service
public class DashboardFacade implements IDashboardFacade {

    @Autowired
    DashboardRepository dashboardRepository;
    @Autowired
    IUserFacade userFacade;

    @Override
    public List<DashboardDTO> getUserDashboards(Long userId) {
        List<DashboardPO> dashboardPOS = dashboardRepository.findAll(filterable -> filterable.eq(DashboardPO.Fields.userId, userId)
        );
        return DashboardConvert.INSTANCE.convertDTOList(dashboardPOS);
    }

    @Override
    public List<DashboardDTO> getDashboardsLike(String keyword, Sort sort) {
        List<String> searchUserIds = new ArrayList<>();
        if (StringUtils.hasText(keyword)) {
            List<UserDTO> userDTOS = userFacade.getUserLike(keyword);
            if (userDTOS != null && !userDTOS.isEmpty()) {
                searchUserIds = userDTOS.stream().map(UserDTO::getUserId).toList();
            }
        }
        List<String> finalSearchUserIds = searchUserIds;
        List<DashboardPO> dashboardPOS = dashboardRepository.findAll(filterable -> filterable.or(filterable1 -> filterable1.likeIgnoreCase(StringUtils.hasText(keyword), DashboardPO.Fields.name, keyword)
                        .in(!finalSearchUserIds.isEmpty(), DashboardPO.Fields.userId, finalSearchUserIds.toArray())), sort);
        return DashboardConvert.INSTANCE.convertDTOList(dashboardPOS);
    }

    @Override
    public List<DashboardDTO> getDashboardsByIds(List<Long> dashboardIds) {
        List<DashboardPO> dashboardPOS = dashboardRepository.findAll(filterable -> filterable.in(DashboardPO.Fields.id, dashboardIds.toArray())
        );
        return DashboardConvert.INSTANCE.convertDTOList(dashboardPOS);
    }

}
