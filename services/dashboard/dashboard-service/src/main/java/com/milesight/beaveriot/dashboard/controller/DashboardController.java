package com.milesight.beaveriot.dashboard.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.canvas.model.dto.CanvasDTO;
import com.milesight.beaveriot.dashboard.model.request.DashboardBatchDeleteRequest;
import com.milesight.beaveriot.dashboard.model.request.DashboardCanvasBatchDeleteRequest;
import com.milesight.beaveriot.dashboard.model.request.DashboardCanvasCreateRequest;
import com.milesight.beaveriot.dashboard.model.request.DashboardInfoRequest;
import com.milesight.beaveriot.dashboard.model.request.SearchDashboardRequest;
import com.milesight.beaveriot.dashboard.model.response.CreateDashboardResponse;
import com.milesight.beaveriot.dashboard.model.response.DashboardCanvasItemResponse;
import com.milesight.beaveriot.dashboard.model.response.DashboardListItemResponse;
import com.milesight.beaveriot.dashboard.model.response.MainDashboardCanvasResponse;
import com.milesight.beaveriot.dashboard.po.DashboardPresetCoverPO;
import com.milesight.beaveriot.dashboard.service.DashboardCoverService;
import com.milesight.beaveriot.dashboard.service.DashboardService;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/14 14:45
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    DashboardService dashboardService;

    @Autowired
    DashboardCoverService dashboardCoverService;

    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_ADD)
    @PostMapping("")
    public ResponseBody<CreateDashboardResponse> createDashboard(@RequestBody @Valid DashboardInfoRequest dashboardInfoRequest) {
        CreateDashboardResponse createDashboardResponse = dashboardService.createDashboard(dashboardInfoRequest);
        return ResponseBuilder.success(createDashboardResponse);
    }

    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_EDIT)
    @PutMapping("/{dashboardId}")
    public ResponseBody<Void> updateDashboard(@PathVariable("dashboardId") Long dashboardId, @RequestBody @Valid DashboardInfoRequest updateDashboardRequest) {
        dashboardService.updateDashboard(dashboardId, updateDashboardRequest);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_DELETE)
    @PostMapping("/batch-delete")
    public ResponseBody<Void> deleteDashboard(@RequestBody DashboardBatchDeleteRequest deleteRequest) {
        dashboardService.deleteDashboard(deleteRequest);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_VIEW)
    @PostMapping("/search")
    public ResponseBody<List<DashboardListItemResponse>> searchDashboards(@RequestBody @Valid SearchDashboardRequest searchRequest) {
        List<DashboardListItemResponse> dashboardResponseList = dashboardService.searchDashboards(searchRequest);
        return ResponseBuilder.success(dashboardResponseList);
    }

    @GetMapping("/covers")
    public ResponseBody<List<DashboardPresetCoverPO>> getCovers() {
        return ResponseBuilder.success(dashboardCoverService.getCovers());
    }

    @GetMapping("/main-canvas")
    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_VIEW)
    public ResponseBody<MainDashboardCanvasResponse> getMainDashboardCanvas() {
        return ResponseBuilder.success(dashboardService.getMainDashboardCanvas());
    }

    @GetMapping("/{dashboardId}/canvas")
    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_VIEW)
    public ResponseBody<List<DashboardCanvasItemResponse>> getDashboardCanvasList(@PathVariable("dashboardId") Long dashboardId) {
        return ResponseBuilder.success(dashboardService.getDashboardCanvasList(dashboardId));
    }

    @PostMapping("/{dashboardId}/canvas/batch-delete")
    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_EDIT)
    public ResponseBody<Void> batchDeleteDashboardCanvas(@PathVariable("dashboardId") Long dashboardId, @RequestBody @Valid DashboardCanvasBatchDeleteRequest request) {
        dashboardService.batchDeleteDashboardCanvas(dashboardId, request.getCanvasIds());
        return ResponseBuilder.success();
    }

    @PostMapping("/{dashboardId}/canvas")
    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_EDIT)
    public ResponseBody<CanvasDTO> createDashboardCanvas(@PathVariable("dashboardId") Long dashboardId, @RequestBody @Valid DashboardCanvasCreateRequest createRequest) {
        return ResponseBuilder.success(dashboardService.createDashboardCanvas(createRequest, dashboardId));
    }

    @PostMapping("/{dashboardId}/home")
    public ResponseBody<Void> setHomeDashboard(@PathVariable("dashboardId") Long dashboardId) {
        dashboardService.setHomeDashboard(dashboardId);
        return ResponseBuilder.success();
    }

    @PostMapping("/{dashboardId}/cancel-home")
    public ResponseBody<Void> cancelSetHomeDashboard(@PathVariable("dashboardId") Long dashboardId) {
        dashboardService.cancelSetHomeDashboard(dashboardId);
        return ResponseBuilder.success();
    }

}
