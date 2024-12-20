package com.milesight.beaveriot.rule.components.variableassigner;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.spi.UriParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author loong
 * @date 2024/12/17 13:49
 */
@RuleNode(value = "variableAssigner", type = RuleNodeType.ACTION, description = "Variable Assigner")
@Data
public class VariableAssignerComponent implements ProcessorNode<Exchange> {

    @UriParam(javaType = "exchangePayload", prefix = "bean")
    private Map<String, Object> exchangePayload;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    @Override
    public void processor(Exchange exchange) {
        Map<String, Object> exchangePayloadVariables = SpELExpressionHelper.resolveExpression(exchange, exchangePayload);
        ExchangePayload payload = ExchangePayload.create(exchangePayloadVariables);

        Map<String, Object> eventEntities = payload.getPayloadsByEntityType(EntityType.EVENT);
        if (!ObjectUtils.isEmpty(eventEntities)) {
            entityValueServiceProvider.saveHistoryRecord(eventEntities);
        }

        // Save property entities
        Map<String, Object> propertyEntities = payload.getPayloadsByEntityType(EntityType.PROPERTY);
        if (!ObjectUtils.isEmpty(propertyEntities)) {
            entityValueServiceProvider.saveValues(propertyEntities);
            entityValueServiceProvider.saveHistoryRecord(propertyEntities);
        }

        // Save service entities， only save history
        Map<String, Object> serviceEntities = payload.getPayloadsByEntityType(EntityType.SERVICE);
        if (!ObjectUtils.isEmpty(serviceEntities)) {
            entityValueServiceProvider.saveHistoryRecord(serviceEntities);
        }

        exchange.getIn().setBody(payload);
    }

}
