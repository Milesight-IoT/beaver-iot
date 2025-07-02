package com.milesight.beaveriot.entity.rule;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author leon
 */

@Slf4j
@Component
@RuleNode(value = RuleNodeNames.innerExchangeSaveAction, description = "innerExchangeSaveAction")
public class GenericExchangeSaveAction implements ProcessorNode<ExchangePayload> {

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Override
    public void processor(ExchangePayload exchange) {
        log.debug("GenericExchangeSaveAction processor {}", exchange.toString());

        entityValueServiceProvider.saveValues(exchange, exchange.getTimestamp());

    }
}
