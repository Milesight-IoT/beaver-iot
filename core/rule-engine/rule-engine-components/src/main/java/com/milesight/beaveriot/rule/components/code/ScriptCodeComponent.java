package com.milesight.beaveriot.rule.components.code;

import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.enums.DataTypeEnums;
import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.spi.UriParam;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * @author leon
 */
@Data
@RuleNode(type = RuleNodeType.ACTION, value = "code", title = "Script Code", description = "Script Code")
public class ScriptCodeComponent implements ProcessorNode<Exchange> {

    @UriParamExtension(uiComponent = "paramAssignInput")
    @UriParam(displayName = "Input Arguments", prefix = "bean")
    private Map<String, Object> inputArguments;

    @UriParamExtension(uiComponent = "codeEditor")
    @UriParam(displayName = "Expression", prefix = "bean")
    private ExpressionNode expression;

    @UriParamExtension(uiComponent = "paramDefineInput")
    @OutputArguments(name = "Payload", displayName = "Output Variables")
    private Map<String, String> payload;

    @Override
    public void processor(Exchange exchange) {

        Map<String, Object> inputVariables = SpELExpressionHelper.resolveExpression(exchange, inputArguments);

        Object result = ExpressionEvaluator.evaluate(expression, exchange, inputVariables, Object.class);

        validatePayload(result);

        exchange.getIn().setBody(result);

    }

    private void validatePayload(Object result) {
        if (ObjectUtils.isEmpty(payload) || !(result instanceof Map resultMap) ) {
            return;
        }

        for (Map.Entry<String, String> entry : payload.entrySet()) {
            if (resultMap.containsKey(entry.getKey()) ) {
                DataTypeEnums dataTypeEnums = DataTypeEnums.valueOf(entry.getKey());
                dataTypeEnums.validate(entry.getKey(), entry.getValue());
            }
        }
    }
}
