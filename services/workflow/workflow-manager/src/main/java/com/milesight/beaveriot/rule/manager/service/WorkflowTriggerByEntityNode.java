package com.milesight.beaveriot.rule.manager.service;

import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.rule.RuleEngineExecutor;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import com.milesight.beaveriot.rule.manager.po.WorkflowPO;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * WorkflowExecuteTriggerNode class.
 *
 * @author simon
 * @date 2024/12/23
 */

@Component
@RuleNode(value = RuleNodeNames.innerWorkflowTriggerByEntity, description = "innerWorkflowTriggerByEntity")
@Slf4j
public class WorkflowTriggerByEntityNode implements ProcessorNode<Exchange> {
    @Autowired
    RuleEngineExecutor ruleEngineExecutor;

    @Autowired
    WorkflowEntityRelationService workflowEntityRelationService;

    @Override
    public void processor(Exchange exchange) {
        Entity serviceEntity = exchange.getIn().getHeader(ExchangeHeaders.DIRECT_EXCHANGE_ENTITY, Entity.class);
        WorkflowPO workflowPO = workflowEntityRelationService.getFlowByEntityId(serviceEntity.getId());
        if (workflowPO == null) {
            log.warn("Cannot find flow id related to entity: {} {}", serviceEntity.getId(), serviceEntity.getKey());
            return;
        }

        if (Boolean.FALSE.equals(workflowPO.getEnabled())) {
            log.info("Workflow {} is disabled.", workflowPO.getId());
            return;
        }

        Map<String, Entity> keyToEntity = serviceEntity.getChildren().stream().collect(Collectors.toMap(Entity::getKey, (childEntity -> childEntity)));
        Object exchangeData = exchange.getIn().getBody();
        if (exchangeData instanceof Map) {
            Map<String, Object> nextExchange = ((Map<String, Object>) exchangeData).entrySet().stream().collect(Collectors.toMap(
                    (Entry<String, Object> entry) -> keyToEntity.get(entry.getKey()).getIdentifier(),
                    (Entry<String, Object> entry) -> Optional.ofNullable(keyToEntity.get(entry.getKey()).getValueType())
                            .orElse(EntityValueType.OBJECT)
                            .convertValue(entry.getValue())
            ));
            exchange.getIn().setBody(nextExchange);
            ruleEngineExecutor.executeWithResponse("direct:" + workflowPO.getId(), exchange);
        } else {
            log.error("Wrong exchange data type, should be a map!");
        }
    }
}
