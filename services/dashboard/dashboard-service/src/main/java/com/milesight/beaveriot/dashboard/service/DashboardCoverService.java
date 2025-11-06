package com.milesight.beaveriot.dashboard.service;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.dashboard.enums.DashboardCoverType;
import com.milesight.beaveriot.dashboard.enums.DashboardErrorCode;
import com.milesight.beaveriot.dashboard.po.DashboardPO;
import com.milesight.beaveriot.dashboard.po.DashboardPresetCoverPO;
import com.milesight.beaveriot.dashboard.repository.DashboardPresetCoverRepository;
import com.milesight.beaveriot.context.model.ResourceRefDTO;
import com.milesight.beaveriot.context.enums.ResourceRefType;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * DashboardCoverService class.
 *
 * @author simon
 * @date 2025/9/8
 */
@Service
public class DashboardCoverService {
    @Autowired
    private ResourceManagerFacade resourceManagerFacade;

    @Autowired
    private DashboardPresetCoverRepository dashboardPresetCoverRepository;

    private ResourceRefDTO getCoverResourceRef(Long dashboardId) {
        return new ResourceRefDTO(dashboardId.toString(), ResourceRefType.DASHBOARD_COVER.name());
    }

    private void validatePresetCover(DashboardCoverType coverType, String coverData) {
        boolean coverExists = dashboardPresetCoverRepository.count(f -> f
                .eq(DashboardPresetCoverPO.Fields.type, coverType.name())
                .eq(DashboardPresetCoverPO.Fields.data, coverData)
        ) > 0;
        if (!coverExists) {
            throw ServiceException.with(DashboardErrorCode.DASHBOARD_PRESET_COVER_NOT_EXIST).build();
        }
    }

    public void applyCover(DashboardPO oldDashboard, DashboardCoverType coverType, String coverData) {
        DashboardCoverType oldCoverType = oldDashboard.getCoverType();
        String oldCoverData = oldDashboard.getCoverData();
        if (Objects.equals(oldCoverType, coverType) && Objects.equals(oldCoverData, coverData)) {
            return;
        }

        oldDashboard.setCoverType(coverType);
        oldDashboard.setCoverData(coverData);
        ResourceRefDTO resourceRefDTO = getCoverResourceRef(oldDashboard.getId());
        if (Objects.equals(oldCoverType, DashboardCoverType.RESOURCE)) {
            resourceManagerFacade.unlinkRef(resourceRefDTO);
        }

        if (Objects.equals(coverType, DashboardCoverType.RESOURCE)) {
            resourceManagerFacade.linkByUrl(coverData, resourceRefDTO);
        } else {
            validatePresetCover(coverType, coverData);
        }
    }

    public void destroyCover(DashboardPO dashboard) {
        if (Objects.equals(dashboard.getCoverType(), DashboardCoverType.RESOURCE)) {
            resourceManagerFacade.unlinkRef(getCoverResourceRef(dashboard.getId()));
        }
    }

    public List<DashboardPresetCoverPO> getCovers() {
        return dashboardPresetCoverRepository.findAll(Sort.by(Sort.Direction.ASC, DashboardPresetCoverPO.Fields.ordered));
    }
}
