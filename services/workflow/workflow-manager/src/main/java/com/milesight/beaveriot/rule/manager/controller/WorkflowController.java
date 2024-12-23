package com.milesight.beaveriot.rule.manager.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.rule.manager.model.request.*;
import com.milesight.beaveriot.rule.manager.model.response.*;
import com.milesight.beaveriot.rule.manager.service.WorkflowLogService;
import com.milesight.beaveriot.rule.manager.service.WorkflowService;
import com.milesight.beaveriot.rule.model.RuleLanguage;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/workflow")
public class WorkflowController {
    @Autowired
    WorkflowService workflowService;

    @Autowired
    WorkflowLogService workflowLogService;

    // Workflow List APIs

    @PostMapping("/flows/search")
    public ResponseBody<Page<WorkflowResponse>> searchWorkflows(@RequestBody SearchWorkflowRequest request) {
        return ResponseBuilder.success(workflowService.search(request));
    }

    @PutMapping("/flows/{flowId}")
    public ResponseBody<Void> updateWorkflowBasicInfo(@PathVariable("flowId") Long flowId, @RequestBody WorkflowBasicInfoRequest request) {
        workflowService.updateBasicInfo(flowId, request.getName(), request.getRemark());
        return ResponseBuilder.success();
    }

    @PostMapping("/flows/batch-delete")
    public ResponseBody<Void> batchDeleteWorkflow(@RequestBody BatchDeleteWorkflowRequest request) {
        List<Long> flowIds = request.getWorkflowIdList().stream().map(Long::valueOf).toList();
        workflowService.batchDelete(flowIds);
        workflowLogService.cleanFlowLogs(flowIds);
        return ResponseBuilder.success();
    }

    @GetMapping("/flows/{flowId}/enable")
    public ResponseBody<Void> enableWorkflow(@PathVariable("flowId") Long flowId) throws Exception {
        workflowService.updateStatus(flowId, true);
        return ResponseBuilder.success();
    }

    @GetMapping("/flows/{flowId}/disable")
    public ResponseBody<Void> disableWorkflow(@PathVariable("flowId") Long flowId) throws Exception {
        workflowService.updateStatus(flowId, false);
        return ResponseBuilder.success();
    }

    // Workflow Log APIs

    @PostMapping("/flows/{flowId}/logs/search")
    public ResponseBody<Page<WorkflowLogResponse>> searchWorkflowLogs(@PathVariable("flowId") Long flowId, @RequestBody SearchWorkflowLogsRequest request) {
        return ResponseBuilder.success(workflowLogService.searchLogs(flowId, request));
    }

    @GetMapping("/flows/logs/{logId}")
    public ResponseBody<WorkflowLogDetailResponse> getWorkflowLogDetail(@PathVariable("logId") Long logId) {
        return ResponseBuilder.success(workflowLogService.getLogDetail(logId));
    }

    // Workflow Design APIs

    @GetMapping("/flows/{flowId}/design")
    public ResponseBody<WorkflowDesignResponse> getWorkflowDesign(@PathVariable("flowId") Long flowId, @RequestParam(value = "version", required = false) Integer version) {
        return ResponseBuilder.success(workflowService.getWorkflowDesign(flowId, version));
    }

    @PostMapping("/flows/design/validate")
    public ResponseBody<Boolean> validateWorkflow(@RequestBody ValidateWorkflowRequest request) {
        return ResponseBuilder.success(workflowService.validateWorkflow(request));
    }

    @PostMapping("/flows/design")
    public ResponseBody<SaveWorkflowResponse> saveWorkflow(@RequestBody SaveWorkflowRequest request) {
        return ResponseBuilder.success(workflowService.saveWorkflow(request));
    }

    @PostMapping("/flows/design/test")
    public ResponseBody<FlowTraceInfo> testWorkflow(@RequestBody TestWorkflowRequest request) {
        return ResponseBuilder.success(workflowService.testWorkflow(request));
    }

    @PostMapping("/flows/node/test")
    public ResponseBody<NodeTraceInfo> testWorkflow(@RequestBody TestWorkflowNodeRequest request) {
        return ResponseBuilder.success(workflowService.testWorkflowNode((request)));
    }

    @GetMapping("/components")
    public ResponseBody<Map<String, List<WorkflowComponentData>>> getWorkflowComponents() {
        return ResponseBuilder.success(workflowService.getWorkflowComponents());
    }

    @GetMapping("/components/{componentId}")
    public ResponseBody<String> getWorkflowComponent(@PathVariable("componentId") String componentId) {
        return ResponseBuilder.success(workflowService.getWorkflowComponentDetail(componentId));
    }

    @GetMapping("/components/languages")
    public ResponseBody<RuleLanguage> getWorkflowComponent() {
        return ResponseBuilder.success(workflowService.getSupportedScriptLanguages());
    }

}
