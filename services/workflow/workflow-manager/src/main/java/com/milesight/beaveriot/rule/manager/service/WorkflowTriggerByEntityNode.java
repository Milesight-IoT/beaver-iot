package com.milesight.beaveriot.rule.manager.service;

import com.google.common.collect.Maps;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.util.ExchangeContextHelper;
import com.milesight.beaveriot.rule.RuleEngineExecutor;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import com.milesight.beaveriot.rule.manager.po.WorkflowPO;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
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

        if (!serviceEntity.getValueType().equals(EntityValueType.OBJECT)) {
            throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Value type of service entity for workflow must be a OBJECT!").build();
        }

        Map<String, Entity> keyToEntity = serviceEntity.getChildren().stream().collect(Collectors.toMap(Entity::getKey, (childEntity -> childEntity)));
        Object exchangeData = exchange.getIn().getBody();
        if (exchangeData instanceof Map) {
            final Map<String, Object> nextExchange = new HashMap<>();
            ((Map<String, Object>) exchangeData).forEach((k, v) -> {
                if (k.equals(serviceEntity.getKey())) {
                    return;
                }

                Entity entity = keyToEntity.get(k);
                Object convertedValue = Optional.ofNullable(entity.getValueType())
                        .orElse(EntityValueType.OBJECT)
                        .convertValue(v);
                if (convertedValue == null) {
                    return;
                }

                nextExchange.put(entity.getIdentifier(), convertedValue);
            });

            Map<String, Object> transmitCamelProperties = ExchangeContextHelper.getTransmitCamelContext(exchange.getProperties());
            Object result = ruleEngineExecutor.executeWithResponse("direct:" + workflowPO.getId(), nextExchange, transmitCamelProperties);
            exchange.getIn().setBody(result);
        } else {
            log.error("Wrong exchange data type, should be a map!");
        }
    }
}
