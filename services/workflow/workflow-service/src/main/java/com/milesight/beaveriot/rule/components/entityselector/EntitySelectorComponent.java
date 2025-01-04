package com.milesight.beaveriot.rule.components.entityselector;

import com.fasterxml.jackson.databind.JsonNode;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.spi.UriParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author loong
 * @date 2024/12/18 8:47
 */
@RuleNode(value = "entitySelector", type = RuleNodeType.ACTION, description = "EntitySelector", testable = false)
@Data
public class EntitySelectorComponent implements ProcessorNode<Exchange> {

    @OutputArguments
    @UriParam(javaType = "java.util.List", prefix = "bean")
    @UriParamExtension(uiComponent = "entityMultipleSelect")
    private List<String> entities;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    @Override
    public void processor(Exchange exchange) {
        List<String> entitiesVariables = (List<String>) SpELExpressionHelper.resolveExpression(exchange, entities);
        Map<String, JsonNode> entityValues = entityValueServiceProvider.findValuesByKeys(entitiesVariables);
        Map<String, Object> entityValuesMap = entityValues.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> JsonUtils.cast(entry.getValue(), Object.class)));
        ExchangePayload exchangePayload = ExchangePayload.create(entityValuesMap);
        exchange.getIn().setBody(exchangePayload);
    }
}
