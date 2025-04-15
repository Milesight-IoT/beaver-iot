package com.milesight.beaveriot.dashboard.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.dashboard.convert.DashboardConvert;
import com.milesight.beaveriot.dashboard.convert.DashboardWidgetConvert;
import com.milesight.beaveriot.dashboard.enums.DashboardErrorCode;
import com.milesight.beaveriot.dashboard.model.dto.DashboardWidgetDTO;
import com.milesight.beaveriot.dashboard.model.request.CreateDashboardRequest;
import com.milesight.beaveriot.dashboard.model.request.UpdateDashboardRequest;
import com.milesight.beaveriot.dashboard.model.response.CreateDashboardResponse;
import com.milesight.beaveriot.dashboard.model.response.DashboardResponse;
import com.milesight.beaveriot.dashboard.po.DashboardHomePO;
import com.milesight.beaveriot.dashboard.po.DashboardPO;
import com.milesight.beaveriot.dashboard.po.DashboardWidgetPO;
import com.milesight.beaveriot.dashboard.repository.DashboardHomeRepository;
import com.milesight.beaveriot.dashboard.repository.DashboardRepository;
import com.milesight.beaveriot.dashboard.repository.DashboardWidgetRepository;
import com.milesight.beaveriot.dashboard.repository.DashboardWidgetTemplateRepository;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author loong
 * @date 2024/10/14 14:46
 */
@Service
public class DashboardService {

    @Autowired
    private DashboardRepository dashboardRepository;
    @Autowired
    private DashboardWidgetRepository dashboardWidgetRepository;
    @Autowired
    private DashboardHomeRepository dashboardHomeRepository;
    @Autowired
    IUserFacade userFacade;

    public CreateDashboardResponse createDashboard(CreateDashboardRequest createDashboardRequest) {
        String name = createDashboardRequest.getName();
        if (!StringUtils.hasText(name)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("name is empty").build();
        }
        Long userId = SecurityUserContext.getUserId();
        DashboardPO dashboardPO = dashboardRepository.findOne(filterable -> filterable.eq(DashboardPO.Fields.name, name)).orElse(null);
        if (dashboardPO != null) {
            throw ServiceException.with(DashboardErrorCode.DASHBOARD_NAME_EXIST).build();
        }
        dashboardPO = new DashboardPO();
        dashboardPO.setId(SnowflakeUtil.nextId());
        dashboardPO.setUserId(userId);
        dashboardPO.setName(name);
        dashboardRepository.save(dashboardPO);

        userFacade.associateResource(userId, ResourceType.DASHBOARD, Collections.singletonList(dashboardPO.getId()));

        CreateDashboardResponse createDashboardResponse = new CreateDashboardResponse();
        createDashboardResponse.setDashboardId(dashboardPO.getId().toString());
        return createDashboardResponse;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDashboard(Long dashboardId, UpdateDashboardRequest updateDashboardRequest) {
        String name = updateDashboardRequest.getName();
        if (!StringUtils.hasText(name)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("name is empty").build();
        }
        DashboardPO otherDashboardPO = dashboardRepository.findOne(filterable -> filterable.eq(DashboardPO.Fields.name, name)).orElse(null);
        if (otherDashboardPO != null && !Objects.equals(otherDashboardPO.getId(), dashboardId)) {
            throw ServiceException.with(DashboardErrorCode.DASHBOARD_NAME_EXIST).build();
        }
        DashboardPO dashboardPO = dashboardRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardPO.Fields.id, dashboardId)).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("dashboard not exist").build());
        dashboardPO.setName(name);
        dashboardRepository.save(dashboardPO);

        List<DashboardWidgetDTO> dashboardWidgetDTOList = updateDashboardRequest.getWidgets();
        List<DashboardWidgetPO> dataDashboardWidgetPOList = dashboardWidgetRepository.findAll(filter -> filter.eq(DashboardWidgetPO.Fields.dashboardId, dashboardId));
        Map<String, DashboardWidgetPO> dashboardWidgetPOMap = new HashMap<>();
        if (dataDashboardWidgetPOList != null && !dataDashboardWidgetPOList.isEmpty()) {
            dashboardWidgetPOMap.putAll(dataDashboardWidgetPOList.stream().collect(Collectors.toMap(t -> String.valueOf(t.getId()), Function.identity())));
        }
        List<DashboardWidgetPO> dashboardWidgetPOList = new ArrayList<>();
        if (dashboardWidgetDTOList != null && !dashboardWidgetDTOList.isEmpty()) {
            dashboardWidgetDTOList.forEach(dashboardWidgetDTO -> {
                String widgetId = dashboardWidgetDTO.getWidgetId();
                Map<String, Object> data = dashboardWidgetDTO.getData();
                if (widgetId == null) {
                    DashboardWidgetPO dashboardWidgetPO = new DashboardWidgetPO();
                    dashboardWidgetPO.setId(SnowflakeUtil.nextId());
                    dashboardWidgetPO.setUserId(dashboardPO.getUserId());
                    dashboardWidgetPO.setTenantId(dashboardPO.getTenantId());
                    dashboardWidgetPO.setDashboardId(dashboardId);
                    dashboardWidgetPO.setData(data);
                    dashboardWidgetPOList.add(dashboardWidgetPO);
                } else {
                    DashboardWidgetPO existDashboardWidgetPO = dashboardWidgetPOMap.get(widgetId);
                    if (existDashboardWidgetPO != null) {
                        existDashboardWidgetPO.setData(data);
                        dashboardWidgetPOList.add(existDashboardWidgetPO);
                    }
                }
            });
        }
        List<Long> dashboardWidgetIdList = dashboardWidgetPOList.stream().map(DashboardWidgetPO::getId).toList();
        List<Long> deleteDashboardWidgetIdList = new ArrayList<>();
        if (dataDashboardWidgetPOList != null && !dataDashboardWidgetPOList.isEmpty()) {
            deleteDashboardWidgetIdList.addAll(dataDashboardWidgetPOList.stream().map(DashboardWidgetPO::getId).filter(id -> !dashboardWidgetIdList.contains(id)).toList());
        }
        if (!deleteDashboardWidgetIdList.isEmpty()) {
            dashboardWidgetRepository.deleteAllById(deleteDashboardWidgetIdList);
        }
        if (!dashboardWidgetPOList.isEmpty()) {
            dashboardWidgetRepository.saveAll(dashboardWidgetPOList);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDashboard(Long dashboardId) {
        dashboardRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardPO.Fields.id, dashboardId)).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("dashboard not exist").build());
        dashboardRepository.deleteById(dashboardId);
        dashboardWidgetRepository.deleteByDashboardId(dashboardId);
        dashboardHomeRepository.deleteByDashboardId(dashboardId);

        userFacade.deleteResource(ResourceType.DASHBOARD, Collections.singletonList(dashboardId));
    }

    public List<DashboardResponse> getDashboards() {
        List<DashboardPO> dashboardPOList;
        try {
            dashboardPOList = dashboardRepository.findAllWithDataPermission().stream().sorted(Comparator.comparing(DashboardPO::getCreatedAt)).collect(Collectors.toList());
        }catch (Exception e) {
            if (e instanceof ServiceException && Objects.equals(((ServiceException) e).getErrorCode(), ErrorCode.FORBIDDEN_PERMISSION.getErrorCode())) {
                return new ArrayList<>();
            }
            throw e;
        }
        if (dashboardPOList.isEmpty()) {
            return new ArrayList<>();
        }
        DashboardHomePO dashboardHomePO = dashboardHomeRepository.findOne(filterable -> filterable.eq(DashboardHomePO.Fields.userId, SecurityUserContext.getUserId())).orElse(null);
        Map<Long, DashboardHomePO> dashboardHomePOMap = new HashMap<>();
        if (dashboardHomePO != null) {
            dashboardHomePOMap.put(dashboardHomePO.getDashboardId(), dashboardHomePO);
        }
        List<DashboardResponse> dashboardResponseList = DashboardConvert.INSTANCE.convertResponseList(dashboardPOList);
        List<Long> dashboardIdList = dashboardResponseList.stream().map(t -> Long.parseLong(t.getDashboardId())).toList();
        List<DashboardWidgetDTO> dashboardWidgetDTOList = getWidgetsByDashBoards(dashboardIdList);
        Map<Long, List<DashboardWidgetDTO>> dashboardWidgetMap = dashboardWidgetDTOList.stream().filter(dashboardWidgetDTO -> dashboardWidgetDTO.getDashboardId() != null).collect(Collectors.groupingBy(DashboardWidgetDTO::getDashboardId));
        dashboardResponseList.forEach(dashboardResponse -> {
            dashboardResponse.setWidgets(dashboardWidgetMap.get(Long.parseLong(dashboardResponse.getDashboardId())));
            dashboardResponse.setHome(dashboardHomePOMap.get(Long.parseLong(dashboardResponse.getDashboardId())) != null);
        });
        return dashboardResponseList;
    }

    private List<DashboardWidgetDTO> getWidgetsByDashBoards(List<Long> dashboardIds) {
        List<DashboardWidgetPO> dashboardWidgetPOList = dashboardWidgetRepository.findAll(filter -> filter.in(DashboardWidgetPO.Fields.dashboardId, dashboardIds.toArray()));
        if (dashboardWidgetPOList == null || dashboardWidgetPOList.isEmpty()) {
            return new ArrayList<>();
        }
        return DashboardWidgetConvert.INSTANCE.convertResponseList(dashboardWidgetPOList);
    }

    @Transactional(rollbackFor = Exception.class)
    public void setHomeDashboard(Long dashboardId) {
        DashboardHomePO dashboardHomePO = dashboardHomeRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardHomePO.Fields.userId, SecurityUserContext.getUserId())).orElse(null);
        if (dashboardHomePO != null) {
            dashboardHomeRepository.delete(dashboardHomePO);
        }
        DashboardHomePO newDashboardPO = new DashboardHomePO();
        newDashboardPO.setId(SnowflakeUtil.nextId());
        newDashboardPO.setUserId(SecurityUserContext.getUserId());
        newDashboardPO.setDashboardId(dashboardId);
        dashboardHomeRepository.save(newDashboardPO);
    }

    public void cancelSetHomeDashboard(Long dashboardId) {
        DashboardHomePO dashboardHomePO = dashboardHomeRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardPO.Fields.userId, SecurityUserContext.getUserId()).eq(DashboardHomePO.Fields.dashboardId, dashboardId)).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("dashboard not exist").build());
        dashboardHomeRepository.delete(dashboardHomePO);
    }

}
