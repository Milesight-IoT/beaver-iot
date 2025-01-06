package com.milesight.beaveriot.rule.manager.service;

import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.rule.RuleEngineExecutor;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import com.milesight.beaveriot.rule.manager.po.WorkflowPO;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Map.Entry;
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
        Entity entity = exchange.getIn().getHeader(ExchangeHeaders.DIRECT_EXCHANGE_ENTITY, Entity.class);
        WorkflowPO workflowPO = workflowEntityRelationService.getFlowByEntityId(entity.getId());
        if (workflowPO == null) {
            log.warn("Cannot find flow id related to entity: {} {}", entity.getId(), entity.getKey());
            return;
        }

        if (Boolean.FALSE.equals(workflowPO.getEnabled())) {
            log.info("Workflow {} is disabled.", workflowPO.getId());
            return;
        }

        Map<String, String> keyToIdentity = entity.getChildren().stream().collect(Collectors.toMap(Entity::getKey, Entity::getIdentifier));
        Object exchangeData = exchange.getIn().getBody();
        if (exchangeData instanceof Map) {
            Map<String, Object> nextExchange = ((Map<String, Object>) exchangeData).entrySet().stream().collect(Collectors.toMap(
                    (Entry<String, Object> entry) -> keyToIdentity.get(entry.getKey()),
                    Entry::getValue
            ));

            ruleEngineExecutor.execute("direct:" + workflowPO.getId(), nextExchange);
        } else {
            log.error("Wrong exchange data type, should be a map!");
        }
    }
}
