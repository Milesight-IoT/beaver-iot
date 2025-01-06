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
import org.apache.camel.spi.InterceptableProcessor;
import org.apache.camel.spi.RouteIdAware;
import org.apache.camel.support.AsyncProcessorSupport;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import java.util.Map;

import static com.milesight.beaveriot.rule.constants.ExchangeHeaders.GRAPH_CHOICE_MATCH_ID;

/**
 * @author leon
 */
@Getter
@Setter
@Slf4j
public class GraphChoiceProcessor extends AsyncProcessorSupport implements Traceable, IdAware, RouteIdAware {

    private final Map<String, WhenDefinition> whenClause;
    private final String otherwiseNodeId;
    private String id;
    private String routeId;

    public GraphChoiceProcessor(Map<String, WhenDefinition> whenClause, String otherwiseNodeId) {
        this.whenClause = whenClause;
        this.otherwiseNodeId = otherwiseNodeId;
        Assert.notNull(whenClause, "whenClause must not be null");
    }

    @Override
    public boolean process(Exchange exchange, AsyncCallback callback) {
        String matchedId = null;
        exchange.getIn().removeHeader(GRAPH_CHOICE_MATCH_ID);

        try {
            for (Map.Entry<String, WhenDefinition> entry : whenClause.entrySet()) {
                WhenDefinition choiceWhenClause = entry.getValue();
                ExpressionDefinition exp = choiceWhenClause.getExpression();
                exp.initPredicate(exchange.getContext());

                Predicate predicate = exp.getPredicate();
                predicate.initPredicate(exchange.getContext());

                boolean matches = predicate.matches(exchange);
                if (matches) {
                    log.debug("doSwitch selected: {}", choiceWhenClause.getLabel());
                    matchedId = entry.getKey();
                    break;
                }
            }

            if (!StringUtils.hasText(matchedId)) {
                log.debug("doSwitch selected: otherwise");
                matchedId = otherwiseNodeId;
            }

            if (StringUtils.hasText(matchedId)) {
                exchange.getIn().setHeader(GRAPH_CHOICE_MATCH_ID, matchedId);
            } else {
                log.debug("doSwitch no when or otherwise selected");
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
