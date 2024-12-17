package com.milesight.beaveriot.rule.components.code;

import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.model.flow.route.ExpressionNode;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.Expression;
import org.apache.camel.spi.UriParam;

import java.util.Map;

import static org.apache.camel.support.builder.ExpressionBuilder.languageExpression;

/**
 * @author leon
 */
@Data
@RuleNode(type = RuleNodeType.ACTION, value = "code", title = "Script Code", description = "Script Code")
public class ScriptCodeComponent implements ProcessorNode<Exchange> {


    @UriParamExtension(uiComponent = "codeEditor")
    @UriParam(displayName = "Expression", prefix = "bean")
    private ExpressionNode expression;

    private Map<String, String> inputArguments;

    @OutputArguments(name = "payload", displayName = "Output Variables")
    private Map<String, String> payload;

    @Override
    public void processor(Exchange exchange) {

        Expression languageExpression = languageExpression(expression.getLanguage(), expression.getExpression());
        languageExpression.init(exchange.getContext());
        Object evaluate = languageExpression.evaluate(exchange, Object.class);
        exchange.getIn().setBody(evaluate);

    }
}
