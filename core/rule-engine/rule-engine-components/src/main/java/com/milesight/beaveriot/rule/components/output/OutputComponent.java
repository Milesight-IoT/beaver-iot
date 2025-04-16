package com.milesight.beaveriot.rule.components.output;

import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.spi.UriParam;

import java.util.Map;

import static com.milesight.beaveriot.rule.constants.RuleNodeNames.CAMEL_OUTPUT;

/**
 * @author leon
 */
@Slf4j
@Data
@RuleNode(type = RuleNodeType.EXTERNAL, value = CAMEL_OUTPUT, title = "Output Node", description = "Output Node")
public class OutputComponent implements ProcessorNode<Exchange> {

    @OutputArguments
    @UriParamExtension(uiComponent = "paramAssignInput")
    @UriParam(prefix = "bean", name = "outputVariables", displayName = "Output Variables")
    private Map<String, Object> outputVariables;

    @Override
    public void processor(Exchange exchange) {
        try {
            Map<String, Object> transformedVariables = SpELExpressionHelper.resolveExpression(exchange, outputVariables);
            exchange.getMessage().setBody(transformedVariables);
        } catch (Exception e) {
            exchange.setException(e);
        }
    }

}
