package com.milesight.beaveriot.entity.rule;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 */

@Slf4j
@Component
@RuleNode(name = RuleNodeNames.innerExchangeSaveAction, description = "innerExchangeSaveAction")
public class GenericExchangeSaveAction implements ProcessorNode<ExchangePayload> {

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Override
    public void processor(ExchangePayload exchange) {
        log.info("GenericExchangeSaveAction processor {}", exchange.toString());

        // Save event entities， only save history
        Map<String, Object> eventEntities = exchange.getPayloadsByEntityType(EntityType.EVENT);
        if (!ObjectUtils.isEmpty(eventEntities)) {
            entityValueServiceProvider.saveHistoryRecord(eventEntities, exchange.getTimestamp());
        }

        // Save property entities
        Map<String, Object> propertyEntities = exchange.getPayloadsByEntityType(EntityType.PROPERTY);
        if (!ObjectUtils.isEmpty(propertyEntities)) {
            entityValueServiceProvider.saveValues(propertyEntities);
            entityValueServiceProvider.saveHistoryRecord(propertyEntities, exchange.getTimestamp());
        }

        // Save service entities， only save history
        Map<String, Object> serviceEntities = exchange.getPayloadsByEntityType(EntityType.SERVICE);
        if (!ObjectUtils.isEmpty(serviceEntities)) {
            entityValueServiceProvider.saveHistoryRecord(serviceEntities, exchange.getTimestamp());
        }

    }
}
