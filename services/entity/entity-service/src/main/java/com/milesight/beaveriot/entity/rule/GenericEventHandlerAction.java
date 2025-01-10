package com.milesight.beaveriot.entity.rule;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.event.ExchangeEvent;
import com.milesight.beaveriot.eventbus.EventBus;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.TransformerNode;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.EXCHANGE_EVENT_TYPE;

/**
 * @author leon
 */
@Slf4j
@Component
@RuleNode(value = RuleNodeNames.innerEventHandlerAction, description = "innerEventHandlerAction")
public class GenericEventHandlerAction implements TransformerNode<ExchangePayload, EventResponse> {

    @Autowired
    private EventBus eventBus;

    @Override
    public EventResponse transform(ExchangePayload exchange) {

        log.debug("GenericEventHandlerAction processor {}", exchange.toString());

        String eventType = (String) exchange.getContext(EXCHANGE_EVENT_TYPE);

        return eventBus.handle(ExchangeEvent.of(eventType, exchange));
    }

}
