package com.milesight.beaveriot.dashboard.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.canvas.enums.CanvasAttachType;
import com.milesight.beaveriot.canvas.facade.ICanvasFacade;
import com.milesight.beaveriot.canvas.model.CanvasDTO;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.dashboard.convert.DashboardConvert;
import com.milesight.beaveriot.dashboard.enums.DashboardErrorCode;
import com.milesight.beaveriot.dashboard.model.request.DashboardBatchDeleteRequest;
import com.milesight.beaveriot.dashboard.model.request.DashboardCanvasCreateRequest;
import com.milesight.beaveriot.dashboard.model.request.DashboardInfoRequest;
import com.milesight.beaveriot.dashboard.model.request.SearchDashboardRequest;
import com.milesight.beaveriot.dashboard.model.response.CreateDashboardResponse;
import com.milesight.beaveriot.dashboard.model.response.DashboardCanvasItemResponse;
import com.milesight.beaveriot.dashboard.model.response.DashboardListItemResponse;
import com.milesight.beaveriot.dashboard.model.response.MainDashboardCanvasResponse;
import com.milesight.beaveriot.dashboard.po.*;
import com.milesight.beaveriot.dashboard.repository.*;
import com.milesight.beaveriot.resource.manager.facade.ResourceManagerFacade;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
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
    private DashboardHomeRepository dashboardHomeRepository;

    @Autowired
    IUserFacade userFacade;

    @Autowired
    ResourceManagerFacade resourceManagerFacade;

    @Autowired
    private EntityServiceProvider entityServiceProvider;

    @Autowired
    DashboardCoverService dashboardCoverService;

    @Autowired
    ICanvasFacade canvasFacade;

    @Transactional(rollbackFor = Throwable.class)
    public CreateDashboardResponse createDashboard(DashboardInfoRequest dashboardInfoRequest) {
        // check dashboard name
        String name = dashboardInfoRequest.getName();
        if (!StringUtils.hasText(name)) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("name is empty").build();
        }
        Long userId = SecurityUserContext.getUserId();
        DashboardPO dashboardPO = dashboardRepository.findOne(filterable -> filterable.eq(DashboardPO.Fields.name, name)).orElse(null);
        if (dashboardPO != null) {
            throw ServiceException.with(DashboardErrorCode.DASHBOARD_NAME_EXIST).build();
        }
        Long dashboardId = SnowflakeUtil.nextId();

        // build default canvas
        CanvasDTO defaultCanvasPO = canvasFacade.createCanvas(dashboardInfoRequest.getName(), CanvasAttachType.DASHBOARD, dashboardId.toString());
        Long defaultCanvasId = defaultCanvasPO.getId();

        // build dashboard
        dashboardPO = new DashboardPO();
        dashboardPO.setId(dashboardId);
        dashboardPO.setUserId(userId);
        dashboardPO.setName(name);
        dashboardPO.setMainCanvasId(defaultCanvasId);
        dashboardPO.setDescription(dashboardInfoRequest.getDescription());
        dashboardCoverService.applyCover(dashboardPO, dashboardInfoRequest.getCoverType(), dashboardInfoRequest.getCoverData());
        dashboardRepository.save(dashboardPO);

        userFacade.associateResource(userId, ResourceType.DASHBOARD, Collections.singletonList(dashboardPO.getId()));

        CreateDashboardResponse createDashboardResponse = new CreateDashboardResponse();
        createDashboardResponse.setDashboardId(dashboardId.toString());
        createDashboardResponse.setMainCanvasId(defaultCanvasId.toString());
        return createDashboardResponse;
    }

    public void updateDashboard(Long dashboardId, DashboardInfoRequest updateDashboardRequest) {
        String name = updateDashboardRequest.getName();
        DashboardPO dashboardPO = dashboardRepository.findOne(filterable -> filterable.eq(DashboardPO.Fields.name, name)).orElse(null);
        if (dashboardPO != null && !Objects.equals(dashboardPO.getId(), dashboardId)) {
            throw ServiceException.with(DashboardErrorCode.DASHBOARD_NAME_EXIST).build();
        }
        if (dashboardPO == null) {
            dashboardPO = dashboardRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardPO.Fields.id, dashboardId))
                    .orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("dashboard not exist").build());
        }

        dashboardPO.setName(updateDashboardRequest.getName());
        dashboardCoverService.applyCover(dashboardPO, updateDashboardRequest.getCoverType(), updateDashboardRequest.getCoverData());
        dashboardRepository.save(dashboardPO);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteDashboard(DashboardBatchDeleteRequest deleteRequest) {
        // get all matched dashboards
        List<DashboardPO> dashboardPOList = dashboardRepository
                .findWithDataPermission(f -> f
                        .in(DashboardPO.Fields.id, deleteRequest.getDashboardIds().toArray())
                );
        List<Long> dashboardIdList = dashboardPOList.stream().map(DashboardPO::getId).toList();
        if (dashboardIdList.isEmpty()) {
            return;
        }

        canvasFacade.deleteCanvasByAttach(CanvasAttachType.DASHBOARD, dashboardIdList.stream().map(Object::toString).toList());

        dashboardHomeRepository.deleteByDashboardIdIn(dashboardIdList);

        dashboardPOList.forEach(dashboardCoverService::destroyCover);
        dashboardRepository.deleteAllById(dashboardIdList);

        userFacade.deleteResource(ResourceType.DASHBOARD, dashboardIdList);
    }

    public List<DashboardListItemResponse> searchDashboards(SearchDashboardRequest searchRequest) {
        List<DashboardPO> dashboardPOList = dashboardRepository
                .findWithDataPermission(f -> f
                        .likeIgnoreCase(StringUtils.hasText(searchRequest.getName()), DashboardPO.Fields.name, searchRequest.getName()))
                .stream().sorted(Comparator.comparing(DashboardPO::getCreatedAt))
                .collect(Collectors.toList());

        if (dashboardPOList.isEmpty()) {
            return new ArrayList<>();
        }
        List<DashboardListItemResponse> dashboardResponseList = DashboardConvert.INSTANCE.convertResponseList(dashboardPOList);
        dashboardHomeRepository
                .findOne(filterable -> filterable.eq(DashboardHomePO.Fields.userId, SecurityUserContext.getUserId()))
                .ifPresentOrElse(
                        dashboardHomePO -> dashboardResponseList.forEach(dashboardResponse -> dashboardResponse.setHome(dashboardHomePO.getDashboardId().equals(Long.parseLong(dashboardResponse.getDashboardId())))),
                        () -> dashboardResponseList.forEach(dashboardResponse -> dashboardResponse.setHome(false))
                );

        dashboardResponseList.sort(Comparator.comparing(DashboardListItemResponse::getHome).reversed().thenComparing(DashboardListItemResponse::getCreatedAt));
        return dashboardResponseList;
    }

    private DashboardPO getDashboardById(Long dashboardId) {
        return dashboardRepository.findOneWithDataPermission(filterable -> filterable.eq(DashboardPO.Fields.id, dashboardId))
                .orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("dashboard not exist").build());
    }

    public CanvasDTO createDashboardCanvas(DashboardCanvasCreateRequest request, Long dashboardId) {
        DashboardPO dashboardPO = getDashboardById(dashboardId);
        return canvasFacade.createCanvas(request.getName(), CanvasAttachType.DASHBOARD, dashboardPO.getId().toString());
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

    public List<DashboardCanvasItemResponse> getDashboardCanvasList(Long dashboardId) {
        DashboardPO dashboardPO = getDashboardById(dashboardId);
        List<CanvasDTO> canvasDTOList = canvasFacade.getCanvasByAttach(CanvasAttachType.DASHBOARD, List.of(dashboardPO.getId().toString()));

        return canvasDTOList.stream().map(canvasDTO -> {
            DashboardCanvasItemResponse response = DashboardConvert.INSTANCE.convertCanvasResponse(canvasDTO);
            response.setIsMain(dashboardPO.getMainCanvasId().equals(canvasDTO.getId()));
            return response;
        }).toList();
    }

    public MainDashboardCanvasResponse getMainDashboardCanvas() {
        MainDashboardCanvasResponse response = new MainDashboardCanvasResponse();
        List<DashboardPO> dashboardPOList = dashboardRepository
                .findAllWithDataPermission()
                .stream().sorted(Comparator.comparing(DashboardPO::getCreatedAt))
                .toList();
        if (dashboardPOList.isEmpty()) {
            return response;
        }

        DashboardPO mainDashboard = null;
        DashboardHomePO dashboardHomePO = dashboardHomeRepository
                .findOne(filterable -> filterable.eq(DashboardHomePO.Fields.userId, SecurityUserContext.getUserId())).orElse(null);
        if (dashboardHomePO != null) {
            for (DashboardPO dashboardPO : dashboardPOList) {
                if (dashboardPO.getId().equals(dashboardHomePO.getDashboardId())) {
                    mainDashboard = dashboardPO;
                }
            }
        }

        if (mainDashboard == null) {
            mainDashboard = dashboardPOList.get(0);
        }

        response.setDashboardId(mainDashboard.getId().toString());
        response.setMainCanvasId(mainDashboard.getMainCanvasId().toString());

        return response;
    }

    public void batchDeleteDashboardCanvas(Long dashboardId, List<Long> canvasIdList) {
        Map<String, DashboardCanvasItemResponse> canvasMap = getDashboardCanvasList(dashboardId).stream().collect(Collectors.toMap(DashboardCanvasItemResponse::getCanvasId, Function.identity()));
        for (Long canvasId: canvasIdList) {
            DashboardCanvasItemResponse canvasData = canvasMap.get(canvasId.toString());
            if (canvasData == null) {
                throw ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("canvas not exist: " + canvasId).build();
            }

            if (Boolean.TRUE.equals(canvasData.getIsMain())) {
                throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).detailMessage("cannot delete main canvas: " + canvasId).build();
            }
        }

        canvasFacade.deleteCanvasByIds(canvasIdList);
    }
}
