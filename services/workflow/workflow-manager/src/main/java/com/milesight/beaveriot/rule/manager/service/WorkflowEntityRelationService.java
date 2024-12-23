package com.milesight.beaveriot.rule.manager.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.EntityBuilder;
import com.milesight.beaveriot.entity.facade.IEntityFacade;
import com.milesight.beaveriot.rule.manager.model.TriggerNodeParameters;
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

    public void saveEntity(WorkflowPO workflowPO, RuleFlowConfig ruleFlowConfig) {
        RuleNodeConfig triggerNodeConfig = null;
        if (ruleFlowConfig != null) {
            for (RuleNodeConfig nodeConfig : ruleFlowConfig.getNodes()) {
                if (nodeConfig.getComponentName().equals("serviceInvocation")) {
                    triggerNodeConfig = nodeConfig;
                    break;
                }
            }
        }

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
                entityFacade.deleteCustomizedEntitiesByIds(List.of(serviceEntity.getId()));
            }

            return;
        }

        TriggerNodeParameters parameters = JsonHelper.cast(triggerNodeConfig.getParameters(), TriggerNodeParameters.class);
        List<Entity> childEntities = parameters.getEntityConfigs().stream().map(entityConfig -> new EntityBuilder()
                .identifier(entityConfig.get("identify"))
                .service(entityConfig.get("name"))
                .valueType(EntityValueType.valueOf(entityConfig.get("type")))
                .build()
        ).toList();

        if (serviceEntity == null) {
            EntityBuilder eb = new EntityBuilder(IntegrationConstants.SYSTEM_INTEGRATION_ID);
            serviceEntity = eb.identifier(workflowPO.getId().toString())
                    .service(workflowPO.getName())
                    .valueType(EntityValueType.OBJECT)
                    .children(childEntities)
                    .build();
            entityServiceProvider.save(serviceEntity);

            serviceEntity = entityServiceProvider.findByKey(serviceEntity.getKey());
            relationPO.setEntityId(serviceEntity.getId());
            relationPO.setFlowId(workflowPO.getId());
            workflowEntityRelationRepository.save(relationPO);
        } else {
            serviceEntity.setName(workflowPO.getName());
            serviceEntity.setChildren(childEntities);
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
            entityFacade.deleteCustomizedEntitiesByIds(entityIds);
        }
    }

    public WorkflowPO getFlowByEntityId(Long entityId) {
        Optional<WorkflowEntityRelationPO> workflowEntityRelationPO = workflowEntityRelationRepository
                .findOne(f -> f.eq(WorkflowEntityRelationPO.Fields.entityId, entityId));
        if (workflowEntityRelationPO.isEmpty()) {
            return null;
        }

        return workflowRepository
                .findById(workflowEntityRelationPO.get().getFlowId())
                .orElse(null);
    }
}
