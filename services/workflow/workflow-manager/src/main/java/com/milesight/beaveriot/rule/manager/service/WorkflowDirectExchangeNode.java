package com.milesight.beaveriot.rule.manager.service;

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

/**
 * WorkflowExecuteTriggerNode class.
 *
 * @author simon
 * @date 2024/12/23
 */

@Component
@RuleNode(value = RuleNodeNames.innerWorkflowDirectExchangeFlow, description = "innerEventHandlerAction")
@Slf4j
public class WorkflowDirectExchangeNode implements ProcessorNode<Exchange> {
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

        ruleEngineExecutor.execute("direct:" + workflowPO.getId(), exchange);
    }
}
