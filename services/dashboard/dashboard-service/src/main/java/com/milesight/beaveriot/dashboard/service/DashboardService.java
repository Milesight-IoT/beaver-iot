package com.milesight.beaveriot.dashboard.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.dashboard.constants.DashboardDataFieldConstants;
import com.milesight.beaveriot.dashboard.convert.DashboardConvert;
import com.milesight.beaveriot.dashboard.convert.DashboardWidgetConvert;
import com.milesight.beaveriot.dashboard.enums.DashboardErrorCode;
import com.milesight.beaveriot.dashboard.model.dto.DashboardWidgetDTO;
import com.milesight.beaveriot.dashboard.model.request.CreateDashboardRequest;
import com.milesight.beaveriot.dashboard.model.request.UpdateDashboardRequest;
import com.milesight.beaveriot.dashboard.model.response.CreateDashboardResponse;
import com.milesight.beaveriot.dashboard.model.response.DashboardListItemResponse;
import com.milesight.beaveriot.dashboard.model.response.DashboardResponse;
import com.milesight.beaveriot.dashboard.po.DashboardEntityPO;
import com.milesight.beaveriot.dashboard.po.DashboardHomePO;
import com.milesight.beaveriot.dashboard.po.DashboardPO;
import com.milesight.beaveriot.dashboard.po.DashboardWidgetPO;
import com.milesight.beaveriot.dashboard.repository.DashboardEntityRepository;
import com.milesight.beaveriot.dashboard.repository.DashboardHomeRepository;
import com.milesight.beaveriot.dashboard.repository.DashboardRepository;
import com.milesight.beaveriot.dashboard.repository.DashboardWidgetRepository;
import com.milesight.beaveriot.entity.dto.EntityQuery;
import com.milesight.beaveriot.entity.dto.EntityResponse;
import com.milesight.beaveriot.entity.facade.IEntityFacade;
import com.milesight.beaveriot.resource.manager.dto.ResourceRefDTO;
import com.milesight.beaveriot.resource.manager.enums.ResourceRefType;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    @Autowired
    ResourceManagerFacade resourceManagerFacade;

    @Autowired
    private DashboardEntityRepository dashboardEntityRepository;

    @Autowired
    private EntityServiceProvider entityServiceProvider;

    @Autowired
    private IEntityFacade entityFacade;

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
        DashboardPO dashboardPO = dashboardRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardPO.Fields.id, dashboardId))
                .orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("dashboard not exist").build());
        dashboardPO.setName(name);
        dashboardRepository.save(dashboardPO);

        dashboardEntityRepository.deleteAllByDashboardId(dashboardId);
        dashboardEntityRepository.flush();
        if (!CollectionUtils.isEmpty(updateDashboardRequest.getEntityIds())) {
            List<Entity> entities = entityServiceProvider.findByIds(updateDashboardRequest.getEntityIds());
            List<DashboardEntityPO> dashboardEntities = entities.stream()
                    .map(e -> DashboardEntityPO.builder()
                            .id(SnowflakeUtil.nextId())
                            .dashboardId(dashboardId)
                            .entityId(e.getId())
                            .entityKey(e.getKey())
                            .build())
                    .toList();
            dashboardEntityRepository.saveAll(dashboardEntities);
        }

        List<DashboardWidgetDTO> dashboardWidgetDTOList = updateDashboardRequest.getWidgets();
        List<DashboardWidgetPO> dataDashboardWidgetPOList = dashboardWidgetRepository.findAll(filter -> filter.eq(DashboardWidgetPO.Fields.dashboardId, dashboardId));
        Map<String, DashboardWidgetPO> dashboardWidgetPOMap = new HashMap<>();
        if (dataDashboardWidgetPOList != null && !dataDashboardWidgetPOList.isEmpty()) {
            dashboardWidgetPOMap.putAll(dataDashboardWidgetPOList.stream().collect(Collectors.toMap(t -> String.valueOf(t.getId()), Function.identity())));
        }
        List<DashboardWidgetPO> dashboardWidgetPOList = new ArrayList<>();
        Map<String, String> deleteUrlMap = new HashMap<>();
        Map<String, String> addUrlMap = new HashMap<>();
        if (dashboardWidgetDTOList != null && !dashboardWidgetDTOList.isEmpty()) {
            dashboardWidgetDTOList.forEach(dashboardWidgetDTO -> {
                String widgetId = dashboardWidgetDTO.getWidgetId();
                Map<String, Object> data = dashboardWidgetDTO.getData();
                String url = getDashboardWidgetUrl(data);
                if (widgetId == null) {
                    DashboardWidgetPO dashboardWidgetPO = new DashboardWidgetPO();
                    dashboardWidgetPO.setId(SnowflakeUtil.nextId());
                    dashboardWidgetPO.setUserId(dashboardPO.getUserId());
                    dashboardWidgetPO.setTenantId(dashboardPO.getTenantId());
                    dashboardWidgetPO.setDashboardId(dashboardId);
                    dashboardWidgetPO.setData(data);
                    dashboardWidgetPOList.add(dashboardWidgetPO);

                    if (StringUtils.hasText(url)) {
                        addUrlMap.put(String.valueOf(dashboardWidgetPO.getId()), url);
                    }
                } else {
                    DashboardWidgetPO existDashboardWidgetPO = dashboardWidgetPOMap.get(widgetId);
                    String originUrl = null;
                    if (existDashboardWidgetPO != null) {
                        originUrl = getDashboardWidgetUrl(existDashboardWidgetPO.getData());

                        existDashboardWidgetPO.setData(data);
                        dashboardWidgetPOList.add(existDashboardWidgetPO);
                    }
                    if (StringUtils.hasText(originUrl) && !originUrl.equals(url)) {
                        deleteUrlMap.put(widgetId, originUrl);
                    }
                    if (StringUtils.hasText(url) && !url.equals(originUrl)) {
                        addUrlMap.put(widgetId, url);
                    }
                }
            });
        }
        List<Long> dashboardWidgetIdList = dashboardWidgetPOList.stream().map(DashboardWidgetPO::getId).toList();
        List<Long> deleteDashboardWidgetIdList = new ArrayList<>();
        if (dataDashboardWidgetPOList != null && !dataDashboardWidgetPOList.isEmpty()) {
            List<DashboardWidgetPO> deleteDashboardWidgetPOList = dataDashboardWidgetPOList.stream().filter(t -> !dashboardWidgetIdList.contains(t.getId())).toList();
            deleteDashboardWidgetIdList.addAll(deleteDashboardWidgetPOList.stream().map(DashboardWidgetPO::getId).toList());
            deleteDashboardWidgetPOList.forEach(t -> {
                String deleteUrl = getDashboardWidgetUrl(t.getData());
                if (StringUtils.hasText(deleteUrl)) {
                    deleteUrlMap.put(String.valueOf(t.getId()), deleteUrl);
                }
            });
        }
        if (!deleteDashboardWidgetIdList.isEmpty()) {
            dashboardWidgetRepository.deleteAllById(deleteDashboardWidgetIdList);
        }
        if (!dashboardWidgetPOList.isEmpty()) {
            dashboardWidgetRepository.saveAll(dashboardWidgetPOList);
        }
        deleteUrlMap.forEach((widgetId, url) -> resourceManagerFacade.unlinkRef(new ResourceRefDTO(widgetId, ResourceRefType.DASHBOARD_WIDGET.name())));
        addUrlMap.forEach((widgetId, url) -> resourceManagerFacade.linkByUrl(url, new ResourceRefDTO(widgetId, ResourceRefType.DASHBOARD_WIDGET.name())));
    }

    private String getDashboardWidgetUrl(Map<String, Object> data) {
        return Optional.ofNullable(data)
                .map(m -> (Map<String, Object>) m.get("config"))
                .map(m -> (Map<String, Object>) m.get("file"))
                .map(m -> m.get("url"))
                .map(Object::toString)
                .orElse(null);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDashboard(Long dashboardId) {
        List<DashboardWidgetPO> dataDashboardWidgetPOList = dashboardWidgetRepository.findAll(filter -> filter.eq(DashboardWidgetPO.Fields.dashboardId, dashboardId));
        Map<String, String> deleteUrlMap = new HashMap<>();
        if (dataDashboardWidgetPOList != null && !dataDashboardWidgetPOList.isEmpty()) {
            dataDashboardWidgetPOList.forEach(dashboardWidgetPO -> {
                String orginUrl = getDashboardWidgetUrl(dashboardWidgetPO.getData());
                if (StringUtils.hasText(orginUrl)) {
                    deleteUrlMap.put(String.valueOf(dashboardWidgetPO.getId()), orginUrl);
                }
            });
        }
        dashboardRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardPO.Fields.id, dashboardId)).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("dashboard not exist").build());
        dashboardRepository.deleteById(dashboardId);
        dashboardWidgetRepository.deleteByDashboardId(dashboardId);
        dashboardHomeRepository.deleteByDashboardId(dashboardId);
        deleteUrlMap.forEach((widgetId, url) -> resourceManagerFacade.unlinkRef(new ResourceRefDTO(widgetId, ResourceRefType.DASHBOARD_WIDGET.name())));

        userFacade.deleteResource(ResourceType.DASHBOARD, Collections.singletonList(dashboardId));
    }

    public DashboardResponse getDashboard(Long dashboardId) {
        DashboardPO dashboardPO = dashboardRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardPO.Fields.id, dashboardId))
                .orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("dashboard not exist").build());
        DashboardResponse response = DashboardConvert.INSTANCE.convertResponse(dashboardPO);

        dashboardHomeRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardHomePO.Fields.userId, SecurityUserContext.getUserId()))
                        .ifPresent(dashboardHomePO -> response.setHome(dashboardId.equals(dashboardHomePO.getDashboardId())));

        List<DashboardWidgetDTO> widgets = getWidgetsByDashBoards(List.of(dashboardId));
        response.setWidgets(widgets);

        List<DashboardEntityPO> dashboardEntityList = dashboardEntityRepository.findAll(filter -> filter.eq(DashboardEntityPO.Fields.dashboardId, dashboardId));
        if (!CollectionUtils.isEmpty(dashboardEntityList)) {
            List<Long> entityIds = dashboardEntityList.stream().map(DashboardEntityPO::getEntityId).toList();
            response.setEntityIds(entityIds.stream().map(Object::toString).toList());

            EntityQuery query = new EntityQuery();
            query.setEntityIds(entityIds);
            query.setPageNumber(1);
            query.setPageSize(DashboardDataFieldConstants.ENTITY_MAX_COUNT_PER_DASHBOARD);
            query.setSort(new Sorts().asc("id"));
            List<EntityResponse> entities = entityFacade.search(query).getContent();
            response.setEntities(entities);
        }

        return response;
    }

    public List<DashboardListItemResponse> getDashboards() {
        List<DashboardPO> dashboardPOList;
        try {
            dashboardPOList = dashboardRepository.findAllWithDataPermission().stream().sorted(Comparator.comparing(DashboardPO::getCreatedAt)).collect(Collectors.toList());
        } catch (Exception e) {
            if (e instanceof ServiceException serviceException
                    && Objects.equals(serviceException.getErrorCode(), ErrorCode.FORBIDDEN_PERMISSION.getErrorCode())) {
                return new ArrayList<>();
            }
            throw e;
        }
        if (dashboardPOList.isEmpty()) {
            return new ArrayList<>();
        }
        DashboardHomePO dashboardHomePO = dashboardHomeRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardHomePO.Fields.userId, SecurityUserContext.getUserId())).orElse(null);
        Map<Long, DashboardHomePO> dashboardHomePOMap = new HashMap<>();
        if (dashboardHomePO != null) {
            dashboardHomePOMap.put(dashboardHomePO.getDashboardId(), dashboardHomePO);
        }
        List<DashboardListItemResponse> dashboardResponseList = DashboardConvert.INSTANCE.convertResponseList(dashboardPOList);
        dashboardResponseList.forEach(dashboardResponse ->
                dashboardResponse.setHome(dashboardHomePOMap.get(Long.parseLong(dashboardResponse.getDashboardId())) != null));
        dashboardResponseList.sort(Comparator.comparing(DashboardListItemResponse::getHome).reversed().thenComparing(DashboardListItemResponse::getCreatedAt));
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
        DashboardHomePO newDashboardHomePO = new DashboardHomePO();
        newDashboardHomePO.setId(SnowflakeUtil.nextId());
        newDashboardHomePO.setUserId(SecurityUserContext.getUserId());
        newDashboardHomePO.setDashboardId(dashboardId);
        dashboardHomeRepository.save(newDashboardHomePO);
    }

    public void cancelSetHomeDashboard(Long dashboardId) {
        DashboardHomePO dashboardHomePO = dashboardHomeRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardHomePO.Fields.userId, SecurityUserContext.getUserId()).eq(DashboardHomePO.Fields.dashboardId, dashboardId)).orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("dashboard not exist").build());
        dashboardHomeRepository.delete(dashboardHomePO);
    }

}
