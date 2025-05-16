package com.milesight.beaveriot.rule.components.entityselector;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import com.milesight.beaveriot.rule.util.WorkflowEntityHelper;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.spi.UriParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;

/**
 * @author loong
 * @date 2024/12/18 8:47
 */
@RuleNode(value = "entitySelector", type = RuleNodeType.ACTION, description = "EntitySelector", testable = false)
@Data
public class EntitySelectorComponent implements ProcessorNode<Exchange> {

    @OutputArguments
    @UriParam(javaType = "java.util.List", prefix = "bean", displayName = "Entity Select Setting")
    @UriParamExtension(uiComponent = "entityMultipleSelect")
    private List<String> entities;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    WorkflowEntityHelper workflowEntityHelper;

    @Override
    public void processor(Exchange exchange) {
        List<String> entitiesVariables = SpELExpressionHelper.resolveExpression(exchange, entities);
        workflowEntityHelper.checkEntityExist(entitiesVariables);
        Map<String, Object> entityValues = entityValueServiceProvider.findValuesByKeys(entitiesVariables);
        ExchangePayload exchangePayload = ExchangePayload.create(entityValues);
        exchange.getIn().setBody(exchangePayload);
    }
}
