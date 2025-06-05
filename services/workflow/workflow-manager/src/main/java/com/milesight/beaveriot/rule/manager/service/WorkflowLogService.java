package com.milesight.beaveriot.rule.manager.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.rule.manager.model.request.SearchWorkflowLogsRequest;
import com.milesight.beaveriot.rule.manager.model.response.WorkflowLogDetailResponse;
import com.milesight.beaveriot.rule.manager.model.response.WorkflowLogResponse;
import com.milesight.beaveriot.rule.manager.po.WorkflowLogDataPO;
import com.milesight.beaveriot.rule.manager.po.WorkflowLogPO;
import com.milesight.beaveriot.rule.manager.po.WorkflowPO;
import com.milesight.beaveriot.rule.manager.repository.WorkflowLogDataRepository;
import com.milesight.beaveriot.rule.manager.repository.WorkflowLogRepository;
import com.milesight.beaveriot.rule.manager.repository.WorkflowRepository;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import com.milesight.beaveriot.rule.support.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
public class WorkflowLogService {
    @Autowired
    WorkflowService workflowService;

    @Autowired
    WorkflowRepository workflowRepository;

    @Autowired
    WorkflowLogRepository workflowLogRepository;

    @Autowired
    WorkflowLogDataRepository workflowLogDataRepository;

    public Page<WorkflowLogResponse> searchLogs(Long flowId, SearchWorkflowLogsRequest request) {
        if (request.getSort().getOrders().isEmpty()) {
            request.sort(new Sorts().desc(WorkflowPO.Fields.id));
        }

        return workflowLogRepository
                .findAll(f -> f
                        .eq(WorkflowLogPO.Fields.flowId, workflowService.getById(flowId).getId())
                        .like(StringUtils.hasText(request.getStatus()), WorkflowLogPO.Fields.status, request.getStatus()), request.toPageable()
                ).map(workflowLogPO -> WorkflowLogResponse.builder()
                        .id(workflowLogPO.getId().toString())
                        .startTime(workflowLogPO.getStartTime())
                        .timeCost(workflowLogPO.getTimeCost())
                        .status(workflowLogPO.getStatus())
                        .version(workflowLogPO.getVersion())
                        .message(workflowLogPO.getMessage())
                        .build()
                );
    }

    public WorkflowLogDetailResponse getLogDetail(Long logId) {
        WorkflowLogPO wl = workflowLogRepository.findById(logId).orElse(null);
        if (wl == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Log not found!").build();
        }

        // check exists
        workflowService.getById(wl.getFlowId());

        WorkflowLogDataPO wld = workflowLogDataRepository.findById(logId).orElse(null);
        if (wld == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Log detail not found!").build();
        }

        return WorkflowLogDetailResponse.builder()
                .id(wl.getId().toString())
                .status(wl.getStatus())
                .timeCost(wl.getTimeCost())
                .startTime(wl.getStartTime())
                .version(wl.getVersion())
                .message(wl.getMessage())
                .traceInfo(JsonHelper.fromJSON(wld.getData(), new TypeReference<List<NodeTraceInfo>>() {}))
                .build();
    }

    public void cleanFlowLogs(List<Long> flowIds) {
        List<Long> workflowIdList = workflowLogRepository
                .findAll(f -> f.in(WorkflowLogPO.Fields.flowId, flowIds.toArray()))
                .stream().map(WorkflowLogPO::getId).toList();
        workflowLogRepository.deleteAllByIdInBatch(workflowIdList);
        workflowLogDataRepository.deleteAllByIdInBatch(workflowIdList);
    }

    @EventListener
    @Async
    public void onFlowLogEvent(FlowTraceInfo event) {
        Long flowId = null;
        try {
            flowId = Long.valueOf(event.getFlowId());
        } catch (NumberFormatException e) {
            log.error("Parse flow id error: {}",event.getFlowId());
            return;
        }

        WorkflowPO workflowPO = workflowRepository.findById(flowId).orElse(null);
        if (workflowPO == null) {
            log.error("Cannot find flow {}", flowId);
            return;
        }

        WorkflowLogPO workflowLogPO = new WorkflowLogPO();
        workflowLogPO.setId(SnowflakeUtil.nextId());
        workflowLogPO.setStatus(event.getStatus().toString());
        workflowLogPO.setFlowId(flowId);
        workflowLogPO.setStartTime(event.getStartTime());
        workflowLogPO.setTimeCost((int) event.getTimeCost());
        workflowLogPO.setTenantId(workflowPO.getTenantId());
        workflowLogPO.setUserId(workflowPO.getUserId());
        workflowLogPO.setMessage(event.getMessage());
        // BUG: Versions may be inconsistent
        workflowLogPO.setVersion(workflowPO.getVersion());

        workflowLogPO = workflowLogRepository.save(workflowLogPO);

        WorkflowLogDataPO workflowLogDataPO = new WorkflowLogDataPO();
        workflowLogDataPO.setId(workflowLogPO.getId());
        workflowLogDataPO.setData(JsonHelper.toJSON(event.getTraceInfos()));
        workflowLogDataRepository.save(workflowLogDataPO);
    }
}
