package com.milesight.beaveriot.rule.manager.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
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
                    (Entry<String, Object> entry) -> tryConvertValue(entry.getValue(), keyToEntity.get(entry.getKey()).getValueType())
            ));

            ruleEngineExecutor.execute("direct:" + workflowPO.getId(), nextExchange);
        } else {
            log.error("Wrong exchange data type, should be a map!");
        }
    }

    private Object tryConvertValue(Object value, EntityValueType type) {
        if (type == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Cannot get trigger param value type.").build();
        }

        switch (type) {
            case STRING:
                return String.valueOf(value.toString());
            case LONG:
                if (value instanceof Number number) {
                    return number.longValue();
                } else {
                    return Long.parseLong(value.toString());
                }
            case DOUBLE:
                if (value instanceof Number number) {
                    return number.doubleValue();
                } else {
                    return Double.parseDouble(value.toString());
                }
            case BOOLEAN:
                if (value instanceof Boolean) {
                    return value;
                } else {
                    return Boolean.parseBoolean(value.toString());
                }
            default:
                throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Unsupported trigger param value type: " + type).build();
        }
    }
}
