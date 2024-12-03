package com.milesight.beaveriot.entity.rule;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.EventBus;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.EVENT_TYPE;

/**
 * @author leon
 */
@Slf4j
@Component
@RuleNode(name = RuleNodeNames.innerEventSubscribeAction, description = "innerEventSubscribeAction")
public class GenericEventSubscribeAction implements ProcessorNode<ExchangePayload> {

    @Autowired
    private EventBus eventBus;

    @Override
    public void processor(ExchangePayload exchange) {

        log.debug("GenericEventSubscribeAction processor {}", exchange.toString());

        String eventType = (String) exchange.getContext(EVENT_TYPE);

        eventBus.publish(ExchangeEvent.of(eventType, exchange));
    }
}
