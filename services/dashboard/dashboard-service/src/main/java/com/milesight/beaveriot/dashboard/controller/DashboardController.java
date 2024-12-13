package com.milesight.beaveriot.dashboard.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.dashboard.model.request.CreateDashboardRequest;
import com.milesight.beaveriot.dashboard.model.request.UpdateDashboardRequest;
import com.milesight.beaveriot.dashboard.model.response.CreateDashboardResponse;
import com.milesight.beaveriot.dashboard.model.response.DashboardResponse;
import com.milesight.beaveriot.dashboard.service.DashboardService;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_ADD)
    @PostMapping("")
    public ResponseBody<CreateDashboardResponse> createDashboard(@RequestBody CreateDashboardRequest createDashboardRequest) {
        CreateDashboardResponse createDashboardResponse = dashboardService.createDashboard(createDashboardRequest);
        return ResponseBuilder.success(createDashboardResponse);
    }

    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_EDIT)
    @PutMapping("/{dashboardId}")
    public ResponseBody<Void> updateDashboard(@PathVariable("dashboardId") Long dashboardId, @RequestBody UpdateDashboardRequest updateDashboardRequest) {
        dashboardService.updateDashboard(dashboardId, updateDashboardRequest);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_EDIT)
    @DeleteMapping("/{dashboardId}")
    public ResponseBody<Void> deleteDashboard(@PathVariable("dashboardId") Long dashboardId) {
        dashboardService.deleteDashboard(dashboardId);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = OperationPermissionCode.DASHBOARD_VIEW)
    @GetMapping("/dashboards")
    public ResponseBody<List<DashboardResponse>> getDashboards() {
        List<DashboardResponse> dashboardResponseList = dashboardService.getDashboards();
        return ResponseBuilder.success(dashboardResponseList);
    }

}
