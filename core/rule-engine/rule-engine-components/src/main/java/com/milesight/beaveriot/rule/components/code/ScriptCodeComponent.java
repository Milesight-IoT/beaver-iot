package com.milesight.beaveriot.rule.components.code;

import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.model.flow.route.ExpressionNode;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.spi.UriParam;

import java.util.Map;

/**
 * @author leon
 */
@Data
@RuleNode(type = RuleNodeType.ACTION, value = "code", title = "Script Code", description = "Script Code")
public class ScriptCodeComponent implements ProcessorNode<Exchange> {

    @UriParamExtension(uiComponent = "codeEditor")
    @UriParam(displayName = "Expression", prefix = "bean")
    private ExpressionNode expression;

    @UriParam(displayName = "Input Arguments", prefix = "bean")
    private Map<String, Object> inputArguments;

    @OutputArguments(name = "Payload", displayName = "Output Variables")
    private Map<String, String> payload;

    @Override
    public void processor(Exchange exchange) {

        Map<String, Object> inputVariables = SpELExpressionHelper.resolveExpression(exchange, inputArguments);

        Object result = ExpressionEvaluator.evaluate(expression, exchange, inputVariables, Object.class);

        exchange.getIn().setBody(result);

    }
}
