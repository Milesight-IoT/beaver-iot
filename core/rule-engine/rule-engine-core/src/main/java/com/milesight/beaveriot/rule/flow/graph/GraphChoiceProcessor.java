package com.milesight.beaveriot.rule.flow.graph;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.AsyncCallback;
import org.apache.camel.Exchange;
import org.apache.camel.Predicate;
import org.apache.camel.Traceable;
import org.apache.camel.model.WhenDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.spi.IdAware;
import org.apache.camel.spi.RouteIdAware;
import org.apache.camel.support.AsyncProcessorSupport;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.milesight.beaveriot.rule.constants.ExchangeHeaders.GRAPH_CHOICE_MATCH_ID;

/**
 * @author leon
 */
@Getter
@Setter
@Slf4j
public class GraphChoiceProcessor extends AsyncProcessorSupport implements Traceable, IdAware, RouteIdAware {

    private final List<Pair<String, WhenDefinition>> whenClause;
    private final String otherwiseNodeId;
    private String id;
    private String routeId;

    public GraphChoiceProcessor(List<Pair<String, WhenDefinition>> whenClause, String otherwiseNodeId) {
        this.whenClause = whenClause;
        this.otherwiseNodeId = otherwiseNodeId;
        Assert.notNull(whenClause, "Choice Node whenClause must not be null");
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        String matchedId = null;
        boolean isMatched = false;
        exchange.getIn().removeHeader(GRAPH_CHOICE_MATCH_ID);

        try {
            for (Pair<String, WhenDefinition> entry : whenClause) {
                WhenDefinition choiceWhenClause = entry.getValue();
                ExpressionDefinition exp = choiceWhenClause.getExpression();
                exp.initPredicate(exchange.getContext());

                Predicate predicate = exp.getPredicate();
                predicate.initPredicate(exchange.getContext());

                boolean matches = predicate.matches(exchange);
                if (matches) {
                    log.debug("Choice node match whenClause branch: {}", choiceWhenClause.getLabel());
                    matchedId = entry.getKey(); // May be null if there is no successor
                    isMatched = true;
                    break;
                }
            }

            if (!isMatched) {
                log.debug("Choice node match otherwise branch : {}", otherwiseNodeId);
                matchedId = otherwiseNodeId;
            }

            if (StringUtils.hasText(matchedId)) {
                exchange.getIn().setHeader(GRAPH_CHOICE_MATCH_ID, matchedId);
            } else {
                log.debug("Choice node no branches matched");
            }
        } catch (Exception ex) {
            exchange.setException(ex);
        } finally {
            callback.done(true);
        }

        return true;
    }

    @Override
    public String toString() {
        return id;
    }

    @Override
    public String getTraceLabel() {
        return "graphChoiceProcessor";
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getRouteId() {
        return routeId;
    }

    @Override
    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

}
