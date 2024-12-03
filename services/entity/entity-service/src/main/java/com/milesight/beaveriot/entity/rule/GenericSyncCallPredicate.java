package com.milesight.beaveriot.entity.rule;

import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.PredicateNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.SYNC_CALL;

/**
 * @author leon
 */
@Slf4j
@Component
@RuleNode(name = "innerSyncCallPredicate", description = "SyncCallPredicate")
public class GenericSyncCallPredicate implements PredicateNode<ExchangePayload> {

    @Override
    public boolean matches(ExchangePayload exchange) {

        Boolean syncCall = exchange.getContext(SYNC_CALL, false);

        log.debug("SyncCallPredicate Predicate {}", syncCall);

        return syncCall;
    }

}
