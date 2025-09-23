package com.milesight.beaveriot.rule.manager.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.rule.manager.model.WorkflowCreateContext;
import com.milesight.beaveriot.rule.manager.model.request.BatchDeleteWorkflowRequest;
import com.milesight.beaveriot.rule.manager.model.request.SaveWorkflowRequest;
import com.milesight.beaveriot.rule.manager.model.request.SearchWorkflowLogsRequest;
import com.milesight.beaveriot.rule.manager.model.request.SearchWorkflowRequest;
import com.milesight.beaveriot.rule.manager.model.request.TestWorkflowNodeRequest;
import com.milesight.beaveriot.rule.manager.model.request.TestWorkflowRequest;
import com.milesight.beaveriot.rule.manager.model.request.ValidateWorkflowRequest;
import com.milesight.beaveriot.rule.manager.model.request.WorkflowBasicInfoRequest;
import com.milesight.beaveriot.rule.manager.model.response.SaveWorkflowResponse;
import com.milesight.beaveriot.rule.manager.model.response.WorkflowComponentData;
import com.milesight.beaveriot.rule.manager.model.response.WorkflowDesignResponse;
import com.milesight.beaveriot.rule.manager.model.response.WorkflowLogDetailResponse;
import com.milesight.beaveriot.rule.manager.model.response.WorkflowLogResponse;
import com.milesight.beaveriot.rule.manager.model.response.WorkflowResponse;
import com.milesight.beaveriot.rule.manager.service.WorkflowLogService;
import com.milesight.beaveriot.rule.manager.service.WorkflowService;
import com.milesight.beaveriot.rule.model.RuleLanguage;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {
    @Autowired
    WorkflowService workflowService;

    @Autowired
    WorkflowLogService workflowLogService;

    // Workflow List APIs
    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_VIEW})
    @PostMapping("/flows/search")
    public ResponseBody<Page<WorkflowResponse>> searchWorkflows(@RequestBody SearchWorkflowRequest request) {
        return ResponseBuilder.success(workflowService.search(request));
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_EDIT})
    @PutMapping("/flows/{flowId}")
    public ResponseBody<Void> updateWorkflowBasicInfo(@PathVariable("flowId") Long flowId, @RequestBody @Valid WorkflowBasicInfoRequest request) {
        workflowService.updateBasicInfo(flowId, request.getName(), request.getRemark());
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_DELETE})
    @PostMapping("/flows/batch-delete")
    public ResponseBody<Void> batchDeleteWorkflow(@RequestBody BatchDeleteWorkflowRequest request) {
        List<Long> flowIds = request.getWorkflowIdList().stream().map(Long::valueOf).toList();
        workflowService.batchDelete(flowIds);
        workflowLogService.cleanFlowLogs(flowIds);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_EDIT})
    @GetMapping("/flows/{flowId}/enable")
    public ResponseBody<Void> enableWorkflow(@PathVariable("flowId") Long flowId) throws Exception {
        workflowService.updateStatus(flowId, true);
        return ResponseBuilder.success();
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_EDIT})
    @GetMapping("/flows/{flowId}/disable")
    public ResponseBody<Void> disableWorkflow(@PathVariable("flowId") Long flowId) throws Exception {
        workflowService.updateStatus(flowId, false);
        return ResponseBuilder.success();
    }

    // Workflow Log APIs

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_VIEW})
    @PostMapping("/flows/{flowId}/logs/search")
    public ResponseBody<Page<WorkflowLogResponse>> searchWorkflowLogs(@PathVariable("flowId") Long flowId, @RequestBody SearchWorkflowLogsRequest request) {
        return ResponseBuilder.success(workflowLogService.searchLogs(flowId, request));
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_VIEW})
    @GetMapping("/flows/logs/{logId}")
    public ResponseBody<WorkflowLogDetailResponse> getWorkflowLogDetail(@PathVariable("logId") Long logId) {
        return ResponseBuilder.success(workflowLogService.getLogDetail(logId));
    }

    // Workflow Design APIs

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.WORKFLOW_VIEW})
    @GetMapping("/flows/{flowId}/design")
    public ResponseBody<WorkflowDesignResponse> getWorkflowDesign(@PathVariable("flowId") Long flowId, @RequestParam(value = "version", required = false) Integer version) {
        return ResponseBuilder.success(workflowService.getWorkflowDesign(flowId, version));
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.WORKFLOW_ADD})
    @PostMapping("/flows/design/validate")
    public ResponseBody<Boolean> validateWorkflow(@RequestBody @Valid ValidateWorkflowRequest request) {
        return ResponseBuilder.success(workflowService.validateWorkflow(request));
    }

    @PostMapping("/flows/design")
    public ResponseBody<SaveWorkflowResponse> saveWorkflow(@RequestBody @Valid SaveWorkflowRequest request) {
        boolean isCreate = request.getId() == null || request.getId().isEmpty();
        if (isCreate) {
            return ResponseBuilder.success(workflowService.createWorkflow(request, null));
        }

        return ResponseBuilder.success(workflowService.updateWorkflow(request));
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.WORKFLOW_ADD})
    @PostMapping("/flows/design/test")
    public ResponseBody<FlowTraceInfo> testWorkflow(@RequestBody @Valid TestWorkflowRequest request) {
        return ResponseBuilder.success(workflowService.testWorkflow(request));
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.WORKFLOW_ADD})
    @PostMapping("/flows/node/test")
    public ResponseBody<NodeTraceInfo> testWorkflowNode(@RequestBody @Valid TestWorkflowNodeRequest request) {
        return ResponseBuilder.success(workflowService.testWorkflowNode((request)));
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.WORKFLOW_ADD})
    @GetMapping("/components")
    public ResponseBody<Map<String, List<WorkflowComponentData>>> getWorkflowComponents() {
        return ResponseBuilder.success(workflowService.getWorkflowComponents());
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.WORKFLOW_ADD})
    @GetMapping("/components/{componentId}")
    public ResponseBody<String> getWorkflowComponent(@PathVariable("componentId") String componentId) {
        return ResponseBuilder.success(workflowService.getWorkflowComponentDetail(componentId));
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_EDIT, OperationPermissionCode.WORKFLOW_ADD})
    @GetMapping("/components/languages")
    public ResponseBody<RuleLanguage> getWorkflowComponent() {
        return ResponseBuilder.success(workflowService.getSupportedScriptLanguages());
    }

}
