package com.milesight.beaveriot.rule.manager.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.entity.facade.IEntityFacade;
import com.milesight.beaveriot.rule.manager.model.TriggerNodeParameters;
import com.milesight.beaveriot.rule.manager.model.WorkflowAdditionalData;
import com.milesight.beaveriot.rule.manager.po.WorkflowEntityRelationPO;
import com.milesight.beaveriot.rule.manager.po.WorkflowPO;
import com.milesight.beaveriot.rule.manager.repository.WorkflowEntityRelationRepository;
import com.milesight.beaveriot.rule.manager.repository.WorkflowRepository;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import com.milesight.beaveriot.rule.model.flow.config.RuleNodeConfig;
import com.milesight.beaveriot.rule.support.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class WorkflowEntityRelationService {
    @Autowired
    WorkflowEntityRelationRepository workflowEntityRelationRepository;

    @Autowired
    WorkflowRepository workflowRepository;

    @Autowired
    EntityServiceProvider entityServiceProvider;

    @Autowired
    IEntityFacade entityFacade;


    @Autowired
    DeviceServiceProvider deviceServiceProvider;

    public RuleNodeConfig getTriggerNode(RuleFlowConfig ruleFlowConfig) {
        if (ruleFlowConfig == null) {
            return null;
        }

        for (RuleNodeConfig nodeConfig : ruleFlowConfig.getNodes()) {
            if (nodeConfig.getComponentName().equals("trigger")) {
                return nodeConfig;
            }
        }

        return null;
    }

    public void saveEntity(WorkflowPO workflowPO, RuleFlowConfig ruleFlowConfig) {
        RuleNodeConfig triggerNodeConfig = getTriggerNode(ruleFlowConfig);

        WorkflowEntityRelationPO relationPO = workflowEntityRelationRepository.findOne(f -> f.eq(WorkflowEntityRelationPO.Fields.flowId, workflowPO.getId())).orElse(null);
        Entity serviceEntity = null;
        if (relationPO != null) {
            serviceEntity = entityServiceProvider.findById(relationPO.getEntityId());
        } else {
            relationPO = new WorkflowEntityRelationPO();
            relationPO.setId(SnowflakeUtil.nextId());
        }

        if (triggerNodeConfig == null) {
            if (serviceEntity != null) {
                entityFacade.deleteEntitiesByIds(List.of(serviceEntity.getId()));
                workflowEntityRelationRepository.delete(relationPO);
            }

            return;
        }

        TriggerNodeParameters parameters = JsonHelper.cast(triggerNodeConfig.getParameters(), TriggerNodeParameters.class);
        List<Entity> childEntities = null;
        if (parameters != null && parameters.getEntityConfigs() != null) {
            childEntities = parameters.getEntityConfigs().stream().
                    map(entityConfig -> {
                        EntityBuilder eb = new EntityBuilder()
                                .identifier(entityConfig.getIdentify())
                                .service(entityConfig.getName())
                                .valueType(entityConfig.getType());
                        boolean optional = !Optional.ofNullable(entityConfig.getRequired()).orElse(false);
                        if (optional) {
                            AttributeBuilder ab = new AttributeBuilder();
                            ab.optional(optional);
                            eb.attributes(ab.build());
                        }
                        return eb.build();
                    }).toList();
        }

        if (serviceEntity == null) {
            // create service entity
            EntityBuilder eb;

            WorkflowAdditionalData additionalData = workflowPO.getAdditionalData();
            // create device entity or custom entity
            if (additionalData == null || additionalData.getDeviceId() == null) {
                eb = new EntityBuilder(IntegrationConstants.SYSTEM_INTEGRATION_ID);
            } else {
                Long relatedDeviceId = Long.valueOf(additionalData.getDeviceId());
                Device relatedDevice = deviceServiceProvider.findById(relatedDeviceId);
                if (relatedDevice == null) {
                    throw ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Device Not Found: " + relatedDeviceId).build();
                }

                eb = new EntityBuilder(relatedDevice.getIntegrationId(), relatedDevice.getKey());

                if (ruleFlowConfig.getMetadata() != null && ruleFlowConfig.getMetadata().getDeviceImportantEntity() != null) {
                    AttributeBuilder ab = new AttributeBuilder();
                    ab.important(ruleFlowConfig.getMetadata().getDeviceImportantEntity());
                    eb.attributes(ab.build());
                }
            }

            serviceEntity = eb.identifier(workflowPO.getId().toString())
                    .service(workflowPO.getName())
                    .valueType(EntityValueType.OBJECT)
                    .children(childEntities)
                    .description(workflowPO.getRemark())
                    .build();
            entityServiceProvider.save(serviceEntity);

            serviceEntity = entityServiceProvider.findByKey(serviceEntity.getKey());
            relationPO.setEntityId(serviceEntity.getId());
            relationPO.setFlowId(workflowPO.getId());
            workflowEntityRelationRepository.save(relationPO);
        } else {
            // update service entity
            serviceEntity.setName(workflowPO.getName());
            serviceEntity.setChildren(childEntities);
            serviceEntity.setDescription(workflowPO.getRemark());
            entityServiceProvider.save(serviceEntity);
        }
    }

    public void deleteEntityByFlowIds(List<Long> flowIds) {
        List<WorkflowEntityRelationPO> relations = workflowEntityRelationRepository
                .findAll(f -> f.in(WorkflowEntityRelationPO.Fields.flowId, flowIds.toArray()));

        if (!relations.isEmpty()) {
            List<Long> entityIds = relations
                    .stream().map(WorkflowEntityRelationPO::getEntityId)
                    .toList();
            workflowEntityRelationRepository.deleteAll(relations);
            entityFacade.deleteEntitiesByIds(entityIds);
        }
    }

    public boolean entityFlowExists(Long entityId) {
        return workflowEntityRelationRepository
                .findOne(f -> f.eq(WorkflowEntityRelationPO.Fields.entityId, entityId)).isPresent();
    }

    public WorkflowPO getFlowByEntityId(Long entityId) {
        Optional<WorkflowEntityRelationPO> workflowEntityRelationPO = workflowEntityRelationRepository
                .findOne(f -> f.eq(WorkflowEntityRelationPO.Fields.entityId, entityId));
        return workflowEntityRelationPO.flatMap(entityRelationPO -> workflowRepository
                .findById(entityRelationPO.getFlowId())).orElse(null);

    }
}
