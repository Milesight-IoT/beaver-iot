package com.milesight.beaveriot.rule.manager.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.GenericPageRequest;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.ValidationUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.event.EntityEvent;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.rule.manager.model.BlueprintDeviceData;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.eventbus.annotations.EventSubscribe;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.pubsub.MessagePubSub;
import com.milesight.beaveriot.rule.RuleEngineComponentManager;
import com.milesight.beaveriot.rule.RuleEngineLifecycleManager;
import com.milesight.beaveriot.rule.manager.model.WorkflowAdditionalData;
import com.milesight.beaveriot.rule.manager.model.WorkflowCreateContext;
import com.milesight.beaveriot.rule.manager.model.event.BaseWorkflowEvent;
import com.milesight.beaveriot.rule.manager.model.event.WorkflowDeployEvent;
import com.milesight.beaveriot.rule.manager.model.event.WorkflowRemoveEvent;
import com.milesight.beaveriot.rule.manager.model.request.SaveWorkflowRequest;
import com.milesight.beaveriot.rule.manager.model.request.SearchWorkflowRequest;
import com.milesight.beaveriot.rule.manager.model.request.TestWorkflowNodeRequest;
import com.milesight.beaveriot.rule.manager.model.request.TestWorkflowRequest;
import com.milesight.beaveriot.rule.manager.model.request.ValidateWorkflowRequest;
import com.milesight.beaveriot.rule.manager.model.response.SaveWorkflowResponse;
import com.milesight.beaveriot.rule.manager.model.response.WorkflowComponentData;
import com.milesight.beaveriot.rule.manager.model.response.WorkflowDesignResponse;
import com.milesight.beaveriot.rule.manager.model.response.WorkflowResponse;
import com.milesight.beaveriot.rule.manager.po.WorkflowHistoryPO;
import com.milesight.beaveriot.rule.manager.po.WorkflowPO;
import com.milesight.beaveriot.rule.manager.repository.WorkflowHistoryRepository;
import com.milesight.beaveriot.rule.manager.repository.WorkflowRepository;
import com.milesight.beaveriot.rule.manager.support.WorkflowTenantCache;
import com.milesight.beaveriot.rule.model.RuleLanguage;
import com.milesight.beaveriot.rule.model.flow.config.RuleEdgeConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.model.trace.FlowTraceInfo;
import com.milesight.beaveriot.rule.model.trace.NodeTraceInfo;
import com.milesight.beaveriot.rule.support.JsonHelper;
import com.milesight.beaveriot.user.facade.IUserFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;
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

    @Autowired
    IDeviceFacade deviceFacade;

    @Autowired
    MessagePubSub messagePubSub;
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
                    .findAllIgnoreTenant(f -> f.eq(WorkflowPO.Fields.enabled, true).isNotNull(WorkflowPO.Fields.designData), pageRequest.toPageable());
            workflowPOPage.forEach(workflowPO -> {
                try {
                    WorkflowDeployEvent workflowDeployEvent = new WorkflowDeployEvent(workflowPO.getTenantId(), workflowPO.getId().toString(), workflowPO.getName(), workflowPO.getDesignData());
                    this.deployFlow(workflowDeployEvent);
                } catch (Exception e) {
                    log.error("Load Workflow Error: {} {} {}", workflowPO.getId(), workflowPO.getName(), e.getMessage());
                } finally {
                    TenantContext.clear();
                }
            });
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

        String requestName = request.getName().trim();
        // get workflows
        Page<WorkflowPO> workflowPOPage = workflowRepository
                .findAllWithDataPermission(f ->
                                f.or(fo -> fo
                                        .likeIgnoreCase(StringUtils.hasText(requestName), WorkflowPO.Fields.name, requestName)
                                        .eq(StringUtils.hasText(requestName) && ValidationUtils.isInteger(requestName), WorkflowPO.Fields.id, requestName)
                                )
                                .eq(request.getEnabled() != null, WorkflowPO.Fields.enabled, request.getEnabled())
                        , request.toPageable());

        // get user nicknames
        Map<String, String> userNicknameMap = new HashMap<>();
        Map<String, DeviceNameDTO> deviceNameMap = new HashMap<>();
        Set<Long> userIds = new HashSet<>();
        Set<Long> deviceIds = new HashSet<>();
        workflowPOPage.forEach(workflowPO -> {
            userIds.add(workflowPO.getUserId());
            WorkflowAdditionalData additionalData = workflowPO.getAdditionalData();
            if (additionalData != null && additionalData.getDeviceId() != null) {
                deviceIds.add(Long.valueOf(additionalData.getDeviceId()));
            }
        });
        if (!userIds.isEmpty()) {
            userFacade.getUserByIds(userIds.stream().toList()).forEach(userDTO -> userNicknameMap.put(userDTO.getUserId(), userDTO.getNickname()));
        }
        if (!deviceIds.isEmpty()) {
            deviceFacade.getDeviceNameByIds(deviceIds.stream().toList()).forEach(deviceNameDTO -> deviceNameMap.put(deviceNameDTO.getId().toString(), deviceNameDTO));
        }

        return workflowPOPage.map(workflowPO -> {
            WorkflowResponse response = WorkflowResponse.builder()
                    .id(workflowPO.getId().toString())
                    .name(workflowPO.getName())
                    .remark(workflowPO.getRemark())
                    .enabled(workflowPO.getEnabled())
                    .updatedAt(workflowPO.getUpdatedAt())
                    .createdAt(workflowPO.getCreatedAt())
                    .userNickname(userNicknameMap.get(workflowPO.getUserId() != null ? workflowPO.getUserId().toString() : ""))
                    .build();
            WorkflowAdditionalData additionalData = workflowPO.getAdditionalData();
            if (additionalData != null && additionalData.getDeviceId() != null) {
                DeviceNameDTO deviceNameDTO = deviceNameMap.get(additionalData.getDeviceId());
                if (deviceNameDTO == null) {
                    return response;
                }

                BlueprintDeviceData deviceDataDTO = new BlueprintDeviceData();
                deviceDataDTO.setId(deviceNameDTO.getId().toString());
                deviceDataDTO.setIdentifier(deviceNameDTO.getIdentifier());
                deviceDataDTO.setName(deviceNameDTO.getName());
                response.setDeviceData(deviceDataDTO);
            }
            return response;
        }
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
            removeSuccessIds.stream().map(Object::toString).forEach(WorkflowTenantCache.INSTANCE::remove);
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

    public void deployFlowAndNotify(WorkflowPO workflowPO) {
        WorkflowDeployEvent workflowDeployEvent = new WorkflowDeployEvent(
                workflowPO.getTenantId(),
                workflowPO.getId().toString(),
                workflowPO.getName(),
                workflowPO.getDesignData()
        );
        deployFlow(workflowDeployEvent);
        messagePubSub.publishAfterCommit(workflowDeployEvent);
    }
    public void deployFlow(WorkflowDeployEvent deployEvent) {
        RuleFlowConfig ruleFlowConfig = parseRuleFlowConfig(deployEvent.getId().toString(), deployEvent.getDesignData());
        ruleFlowConfig.setName(deployEvent.getName());
        if (ruleFlowConfig == null) {
            removeFlow(deployEvent);
            return;
        }
        String tenantId = ObjectUtils.isEmpty(deployEvent.getTenantId()) ? TenantContext.getTenantId() : deployEvent.getTenantId();
        TenantContext.setTenantId(tenantId);
        ruleEngineLifecycleManager.deployFlow(ruleFlowConfig);
        WorkflowTenantCache.INSTANCE.put(deployEvent.getId().toString(), tenantId);
    }

    public void removeFlowAndNotify(WorkflowPO workflowPO) {
        WorkflowRemoveEvent workflowRemoveEvent = new WorkflowRemoveEvent(workflowPO.getTenantId(), workflowPO.getId().toString());
        removeFlow(workflowRemoveEvent);
        messagePubSub.publishAfterCommit(workflowRemoveEvent);
    }

    public void removeFlow(BaseWorkflowEvent removeEvent) {
        ruleEngineLifecycleManager.removeFlow(removeEvent.getId().toString());
        WorkflowTenantCache.INSTANCE.remove(removeEvent.getId().toString());
    }

    public void disableFlowImmediately(Long flowId) {
        WorkflowPO wp = getById(flowId);
        if (!wp.getEnabled()) {
            return;
        }
        ruleEngineLifecycleManager.removeFlowImmediately(wp.getId().toString());
        wp.setEnabled(false);
        workflowRepository.save(wp);
    }

    public void updateStatus(Long flowId, boolean status) {
        assertWorkflowPrepared();

        WorkflowPO wp = getById(flowId);

        if (wp.getEnabled().equals(status)) {
            return;
        }

        if (status) {
            deployFlowAndNotify(wp);
        } else {
            removeFlowAndNotify(wp);
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
                .additionalData(workflowPO.getAdditionalData())
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

    private boolean isRuleFlowEqual(RuleFlowConfig config1, RuleFlowConfig config2) {
        if (config1 == config2) {
            return true;
        }

        if (config1 == null || config2 == null) {
            return false;
        }

        final List<RuleNodeConfig> config1Nodes = Optional.ofNullable(config1.getNodes()).orElse(new ArrayList<>());
        final List<RuleNodeConfig> config2Nodes = Optional.ofNullable(config2.getNodes()).orElse(new ArrayList<>());

        if (config1Nodes.size() != config2Nodes.size()) {
            return false;
        }

        final List<RuleEdgeConfig> config1Edges = Optional.ofNullable(config1.getEdges()).orElse(new ArrayList<>());
        final List<RuleEdgeConfig> config2Edges = Optional.ofNullable(config2.getEdges()).orElse(new ArrayList<>());

        if (config1Edges.size() != config2Edges.size()) {
            return false;
        }

        for (int i = 0; i < config1Nodes.size(); i++) {
            RuleNodeConfig ruleNode1 = config1Nodes.get(i);
            RuleNodeConfig ruleNode2 = config2Nodes.get(i);
            if (
                    !Objects.equals(ruleNode1.getId(), ruleNode2.getId())
                            || !Objects.equals(ruleNode1.getComponentName(), ruleNode2.getComponentName())
                            || !Objects.equals(ruleNode1.getParameters(), ruleNode2.getParameters())
            ) {
                return false;
            }
        }

        for (int i = 0; i < config1Edges.size(); i++) {
            if (!Objects.equals(config1Edges.get(i), config2Edges.get(i))) {
                return false;
            }
        }

        return true;
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_ADD})
    @Transactional(rollbackFor = Exception.class)
    public SaveWorkflowResponse createWorkflow(SaveWorkflowRequest request, WorkflowCreateContext createContext) {
        assertWorkflowPrepared();
        WorkflowPO workflowPO = new WorkflowPO();
        workflowPO.setId(SnowflakeUtil.nextId());
        workflowPO.setUserId(SecurityUserContext.getUserId());
        workflowPO.setVersion(1);
        workflowPO.setUpdatedUser(SecurityUserContext.getUserId());
        workflowPO.setName(request.getName());
        workflowPO.setRemark(request.getRemark());
        workflowPO.setEnabled(request.getEnabled());
        workflowPO.setDesignData(request.getDesignData());

        if (createContext != null && createContext.getDeviceId() != null) {
            WorkflowAdditionalData additionalData = new WorkflowAdditionalData();
            additionalData.setDeviceId(createContext.getDeviceId().toString());
            workflowPO.setAdditionalData(additionalData);
        }

        final String workflowIdStr = workflowPO.getId().toString();
        RuleFlowConfig ruleFlowConfig = parseRuleFlowConfig(workflowIdStr, request.getDesignData());

        if (Boolean.TRUE.equals(workflowPO.getEnabled())) {
            deployFlowAndNotify(workflowPO);
        } else if (ruleFlowConfig != null) {
            ruleEngineLifecycleManager.validateFlow(ruleFlowConfig);
        }

        workflowPO = workflowRepository.save(workflowPO);

        // Build Response
        SaveWorkflowResponse swr = new SaveWorkflowResponse();
        swr.setFlowId(workflowIdStr);
        swr.setVersion(workflowPO.getVersion());

        workflowEntityRelationService.saveEntity(workflowPO, ruleFlowConfig);

        return swr;
    }

    @OperationPermission(codes = {OperationPermissionCode.WORKFLOW_EDIT})
    @Transactional(rollbackFor = Exception.class)
    public SaveWorkflowResponse updateWorkflow(SaveWorkflowRequest request) {
        assertWorkflowPrepared();
        WorkflowPO workflowPO = getById(Long.valueOf(request.getId()));
        if (!workflowPO.getVersion().equals(request.getVersion())) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Version Expired: " + request.getVersion()).build();
        }

        WorkflowHistoryPO workflowHistoryPO = new WorkflowHistoryPO();
        workflowHistoryPO.setId(SnowflakeUtil.nextId());
        workflowHistoryPO.setFlowId(workflowPO.getId());
        workflowHistoryPO.setUserId(workflowPO.getUpdatedUser());
        workflowHistoryPO.setVersion(workflowPO.getVersion());
        workflowHistoryPO.setDesignData(workflowPO.getDesignData());

        workflowPO.setUpdatedUser(SecurityUserContext.getUserId());
        workflowPO.setName(request.getName());
        workflowPO.setRemark(request.getRemark());

        // Inc data version if design changed
        Integer beforeVersion = workflowPO.getVersion();
        if (!Objects.equals(workflowPO.getDesignData(), request.getDesignData())) {
            workflowPO.setVersion(beforeVersion + 1);
        }

        final String workflowIdStr = workflowPO.getId().toString();
        RuleFlowConfig ruleFlowConfig = parseRuleFlowConfig(workflowIdStr, request.getDesignData());
        final boolean isFlowUpdated = !isRuleFlowEqual(parseRuleFlowConfig(workflowIdStr, workflowPO.getDesignData()), ruleFlowConfig);
        workflowPO.setDesignData(request.getDesignData());

        boolean isEnableUpdated = !Objects.equals(workflowPO.getEnabled(), request.getEnabled());
        workflowPO.setEnabled(request.getEnabled());
        if (Boolean.TRUE.equals(workflowPO.getEnabled()) && (isFlowUpdated || isEnableUpdated)) {
            // a workflow would be validated at the time it is deployed
            deployFlowAndNotify(workflowPO);
        } else if (Boolean.FALSE.equals(workflowPO.getEnabled())) {
            if (isEnableUpdated) {
                removeFlowAndNotify(workflowPO);
            }

            if (isFlowUpdated && ruleFlowConfig != null) {
                ruleEngineLifecycleManager.validateFlow(ruleFlowConfig);
            }
        }

        // Save workflow and history
        workflowPO = workflowRepository.save(workflowPO);
        if (!workflowPO.getVersion().equals(beforeVersion)) {
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


    @EventSubscribe(eventType = EntityEvent.EventType.DELETED, payloadKeyExpression = "*")
    public void onEntityDeleted(EntityEvent entityEvent) {
        Entity entity = entityEvent.getPayload();
        if (!entity.getIntegrationId().equals(IntegrationConstants.SYSTEM_INTEGRATION_ID)) {
            return;
        }

        if (StringUtils.hasLength(entity.getParentKey())) {
            return;
        }

        WorkflowPO workflowPO = workflowEntityRelationService.getFlowByEntityId(entity.getId());

        if (workflowPO == null) {
            return;
        }

        if (workflowEntityRelationService.getTriggerNode(parseRuleFlowConfig(workflowPO.getId().toString(), workflowPO.getDesignData())) != null) {
            ((WorkflowService) AopContext.currentProxy()).batchDelete(List.of(workflowPO.getId()));
        }
    }
}
