package com.milesight.beaveriot.rule.manager.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.GenericPageRequest;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.rule.RuleEngineComponentManager;
import com.milesight.beaveriot.rule.RuleEngineLifecycleManager;
import com.milesight.beaveriot.rule.manager.model.request.*;
import com.milesight.beaveriot.rule.manager.model.response.*;
import com.milesight.beaveriot.rule.manager.po.WorkflowHistoryPO;
import com.milesight.beaveriot.rule.manager.po.WorkflowPO;
import com.milesight.beaveriot.rule.manager.repository.*;
import com.milesight.beaveriot.rule.model.RuleLanguage;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import com.milesight.beaveriot.rule.support.JsonHelper;
import com.milesight.beaveriot.user.facade.IUserFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Slf4j
public class WorkflowService {
    @Autowired
    WorkflowRepository workflowRepository;

    @Autowired
    WorkflowHistoryRepository workflowHistoryRepository;

    @Autowired
    RuleEngineLifecycleManager ruleEngineLifecycleManager;

    @Autowired
    RuleEngineComponentManager ruleEngineComponentManager;

    @Autowired
    IUserFacade userFacade;

    @Autowired
    WorkflowEntityRelationService workflowEntityRelationService;

    private final AtomicBoolean workflowPrepared = new AtomicBoolean(true);

    @Async
    public void loadActiveWorkflows() {
        if (!workflowPrepared.compareAndSet(true, false)) {
            return;
        }

        GenericPageRequest pageRequest = new GenericPageRequest();
        pageRequest.sort(new Sorts().desc(WorkflowPO.Fields.id));
        final int pageSize = 1000;
        pageRequest.setPageSize(pageSize);
        pageRequest.setPageNumber(1);
        Page<WorkflowPO> workflowPOPage;
        do {
            workflowPOPage = workflowRepository
                    .findAll(f -> f.eq(WorkflowPO.Fields.enabled, true).isNotNull(WorkflowPO.Fields.routeData), pageRequest.toPageable());
            workflowPOPage.forEach((this::deployFlow));
            pageRequest.setPageNumber(pageRequest.getPageNumber() + 1);
        } while (workflowPOPage.hasNext());

        workflowPrepared.set(true);
    }

    private void assertWorkflowPrepared() {
        if (!workflowPrepared.get()) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Workflow initialization not completed!").build();
        }
    }

    public WorkflowPO getById(Long flowId) {
        Optional<WorkflowPO> wp = workflowRepository.findByIdWithDataPermission(flowId);
        if (wp.isEmpty()) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).build();
        }

        return wp.get();
    }

    public Page<WorkflowResponse> search(SearchWorkflowRequest request) {
        if (request.getSort().getOrders().isEmpty()) {
            request.sort(new Sorts().desc(WorkflowPO.Fields.id));
        }

        // get workflows
        Page<WorkflowPO> workflowPOPage = workflowRepository
                .findAllWithDataPermission(f -> f.like(StringUtils.hasText(request.getName()), WorkflowPO.Fields.name, request.getName()), request.toPageable());

        // get user nicknames
        Map<String, String> userNicknameMap = new HashMap<>();
        List<Long> userIds = workflowPOPage.map(WorkflowPO::getUserId).stream().distinct().toList();
        if (!userIds.isEmpty()) {
            userFacade.getUserByIds(userIds).forEach(userDTO -> userNicknameMap.put(userDTO.getUserId(), userDTO.getNickname()));
        }

        return workflowPOPage.map(workflowPO -> WorkflowResponse.builder()
                .id(workflowPO.getId().toString())
                .name(workflowPO.getName())
                .remark(workflowPO.getRemark())
                .enabled(workflowPO.getEnabled())
                .updatedAt(workflowPO.getUpdatedAt())
                .createdAt(workflowPO.getCreatedAt())
                .userNickname(userNicknameMap.get(workflowPO.getUserId().toString()))
                .build()
        );
    }

    public void updateBasicInfo(Long flowId, String name, String remark) {
        WorkflowPO wp = getById(flowId);

        wp.setName(name);
        wp.setRemark(remark);

        workflowRepository.save(wp);
    }

    @Transactional
    public void batchDelete(List<Long> flowIds) {
        assertWorkflowPrepared();

        List<WorkflowPO> workflows = workflowRepository.findByIdInWithDataPermission(flowIds);
        List<WorkflowPO> removeSuccess = new ArrayList<>();
        List<WorkflowPO> removeFailure = new ArrayList<>();
        workflows.forEach(f -> {
            try {
                ruleEngineLifecycleManager.removeFlow(f.getId().toString());
                removeSuccess.add(f);
            } catch (Exception e) {
                log.error("Remove rule engine failed: {} {}", f.getId(), e.getMessage());
                removeFailure.add(f);
            }
        });

        if (!removeSuccess.isEmpty()) {
            List<Long> removeSuccessIds = removeSuccess.stream().map(WorkflowPO::getId).toList();

            workflowEntityRelationService.deleteEntityByFlowIds(removeSuccessIds);
            workflowRepository.deleteAll(removeSuccess);
            workflowHistoryRepository.deleteByFlowIdIn(removeSuccessIds);
        }

        if (!removeFailure.isEmpty()) {
            String failedFlows = String.join(", ", removeFailure.stream().map(WorkflowPO::getName).toList());
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Some failed: " + failedFlows).build();
        }
    }

    private RuleFlowConfig parseRuleFlowConfig(String flowId, String designData) {
        if (!StringUtils.hasLength(designData)) {
            return null;
        }

        RuleFlowConfig ruleFlowConfig = JsonHelper.fromJSON(designData, RuleFlowConfig.class);
        if (ruleFlowConfig == null || ruleFlowConfig.getNodes() == null || ruleFlowConfig.getNodes().isEmpty()) {
            return null;
        }

        ruleFlowConfig.setFlowId(flowId);
        return ruleFlowConfig;
    }

    private void deployFlow(WorkflowPO wp) {
        if (wp.getRouteData() == null) {
            removeFlow(wp);
            return;
        }

        ruleEngineLifecycleManager.deployFlow(wp.getId().toString(), wp.getRouteData());
    }

    private void removeFlow(WorkflowPO wp) {
        ruleEngineLifecycleManager.removeFlow(wp.getId().toString());
    }

    public void updateStatus(Long flowId, boolean status) {
        assertWorkflowPrepared();

        WorkflowPO wp = getById(flowId);

        if (wp.getEnabled().equals(status)) {
            return;
        }

        if (status) {
            deployFlow(wp);
        } else {
            removeFlow(wp);
        }

        wp.setEnabled(status);

        workflowRepository.save(wp);
    }

    /**
     * Get specific version of workflow design data
     *
     * @param flowId workflow id
     * @param version workflow version. Current version if null
     * @return
     */
    public WorkflowDesignResponse getWorkflowDesign(Long flowId, Integer version) {
        WorkflowPO workflowPO = getById(flowId);
        WorkflowDesignResponse response = WorkflowDesignResponse.builder()
                .id(workflowPO.getId().toString())
                .name(workflowPO.getName())
                .remark(workflowPO.getRemark())
                .enabled(workflowPO.getEnabled())
                .designData(workflowPO.getDesignData())
                .version(workflowPO.getVersion())
                .build();
        if (version != null) {
            if (version > workflowPO.getVersion()) {
                throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Invalid Version: " + version).build();
            } else if (version < workflowPO.getVersion()) {
                WorkflowHistoryPO workflowHistoryPO = workflowHistoryRepository.findOne(f -> f
                        .eq(WorkflowHistoryPO.Fields.flowId, flowId)
                        .eq(WorkflowHistoryPO.Fields.version, version))
                        .orElseThrow(() -> ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Version Not Found: " + version).build());
                response.setDesignData(workflowHistoryPO.getDesignData());
            }
        }

        return response;
    }

    public boolean validateWorkflow(ValidateWorkflowRequest request) {
        return ruleEngineLifecycleManager.validateFlow(JsonHelper.fromJSON(request.getDesignData(), RuleFlowConfig.class));
    }

//    @Transactional(rollbackFor = Exception.class)
    public SaveWorkflowResponse saveWorkflow(SaveWorkflowRequest request) {
        assertWorkflowPrepared();

        boolean isCreate = request.getId() == null || request.getId().isEmpty();
        // Save Workflow Data
        WorkflowPO workflowPO;
        WorkflowHistoryPO workflowHistoryPO = null;
        if (isCreate) {
            workflowPO = new WorkflowPO();
            workflowPO.setId(SnowflakeUtil.nextId());
            workflowPO.setUserId(SecurityUserContext.getUserId());
            workflowPO.setVersion(1);
        } else {
            workflowPO = getById(Long.valueOf(request.getId()));
            if (!workflowPO.getVersion().equals(request.getVersion())) {
                throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Version Expired: " + request.getVersion()).build();
            }

            // init history
            workflowHistoryPO = new WorkflowHistoryPO();
            workflowHistoryPO.setId(SnowflakeUtil.nextId());
            workflowHistoryPO.setFlowId(workflowPO.getId());
            workflowHistoryPO.setUserId(workflowPO.getUpdatedUser());
            workflowHistoryPO.setVersion(workflowPO.getVersion());
            workflowHistoryPO.setDesignData(workflowPO.getDesignData());
        }

        workflowPO.setUpdatedUser(SecurityUserContext.getUserId());
        workflowPO.setName(request.getName());
        workflowPO.setRemark(request.getRemark());

        // Inc data version if design changed
        Integer beforeVersion = workflowPO.getVersion();
        if (!isCreate && !Objects.equals(workflowPO.getDesignData(), request.getDesignData())) {
            workflowPO.setVersion(beforeVersion + 1);
        }

        workflowPO.setDesignData(request.getDesignData());

        final String workflowIdStr = workflowPO.getId().toString();
        RuleFlowConfig ruleFlowConfig = parseRuleFlowConfig(workflowIdStr, request.getDesignData());
        String previousRoutingData = workflowPO.getRouteData();
        if (ruleFlowConfig != null) {
            workflowPO.setRouteData(ruleEngineLifecycleManager.generateRouteFlow(ruleFlowConfig));
        } else {
            workflowPO.setRouteData(null);
        }

        boolean isEnableUpdated = !Objects.equals(workflowPO.getEnabled(), request.getEnabled());
        boolean isRoutingUpdated = !Objects.equals(workflowPO.getRouteData(), previousRoutingData);

        // Deploy or Remove
        workflowPO.setEnabled(request.getEnabled());
        if (Boolean.TRUE.equals(workflowPO.getEnabled()) && (isRoutingUpdated || isEnableUpdated)) {
            deployFlow(workflowPO);
        } else if (Boolean.FALSE.equals(workflowPO.getEnabled()) && isEnableUpdated) {
            removeFlow(workflowPO);
        }

        // Save workflow and history
        workflowPO = workflowRepository.save(workflowPO);
        if (workflowHistoryPO != null && !workflowPO.getVersion().equals(beforeVersion)) {
            workflowHistoryRepository.save(workflowHistoryPO);
        }

        // Build Response
        SaveWorkflowResponse swr = new SaveWorkflowResponse();
        swr.setFlowId(workflowIdStr);
        swr.setVersion(workflowPO.getVersion());

        workflowEntityRelationService.saveEntity(workflowPO, ruleFlowConfig);

        return swr;
    }

    public FlowTraceInfo testWorkflow(TestWorkflowRequest request) {
        return ruleEngineLifecycleManager.trackFlow(JsonHelper.fromJSON(request.getDesignData(), RuleFlowConfig.class), request.getInput());
    }

    public NodeTraceInfo testWorkflowNode(TestWorkflowNodeRequest request) {
        return ruleEngineLifecycleManager.trackNode(JsonHelper.fromJSON(request.getNodeConfig(), RuleNodeConfig.class), request.getInput());
    }

    public Map<String, List<WorkflowComponentData>> getWorkflowComponents() {
        Map<String, List<WorkflowComponentData>> componentMap = new HashMap<>();
        ruleEngineComponentManager.getDeclaredComponents().forEach((key, value) -> {
            List<WorkflowComponentData> componentGroup = new ArrayList<>();
            value.forEach(componentDef -> {
                WorkflowComponentData wc = new WorkflowComponentData();
                wc.setName(componentDef.getName());
                wc.setTitle(componentDef.getTitle());
                try {
                    wc.setData(ruleEngineComponentManager.getComponentDefinitionSchema(componentDef.getName()));
                } catch (IllegalArgumentException e) {
                    log.warn("List components failed: " + e.getMessage());
                }

                componentGroup.add(wc);
            });
            componentMap.put(key, componentGroup);
        });

        return componentMap;
    }

    public String getWorkflowComponentDetail(String componentId) {
        return ruleEngineComponentManager.getComponentDefinitionSchema(componentId);
    }

    public RuleLanguage getSupportedScriptLanguages() {
        return ruleEngineComponentManager.getDeclaredLanguages();
    }
}
