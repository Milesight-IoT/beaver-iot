package com.milesight.beaveriot.rule.components.code;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.model.OutputVariablesSettings;
import com.milesight.beaveriot.rule.model.flow.ExpressionNode;
import com.milesight.beaveriot.rule.support.JsonHelper;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.spi.UriParam;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
@Data
@RuleNode(type = RuleNodeType.ACTION, value = "code", title = "Script Code", description = "Script Code")
public class ScriptCodeComponent implements ProcessorNode<Exchange> {

    @UriParamExtension(uiComponent = "paramAssignInput", loggable = true)
    @UriParam(displayName = "Input Arguments", prefix = "bean")
    private Map<String, Object> inputArguments;

    @UriParamExtension(uiComponent = "codeEditor")
    @UriParam(displayName = "Expression", prefix = "bean")
    private ExpressionNode expression;

    @UriParamExtension(uiComponent = "paramDefineInput")
    @OutputArguments(displayName = "Output Variables")
    @UriParam(prefix = "bean", name = "payload", displayName = "Output Variables")
    private List<OutputVariablesSettings> payload;

    @Override
    public void processor(Exchange exchange) {

        Map<String, Object> inputVariables = SpELExpressionHelper.resolveExpression(exchange, inputArguments);

        Object result = ExpressionEvaluator.evaluate(expression, exchange, inputVariables, Object.class);

        if (!ObjectUtils.isEmpty(payload) && !(result instanceof Map)) {
            exchange.getIn().setBody(Map.of());
        } else {
            OutputVariablesSettings.validate(result, payload);
            exchange.getIn().setBody(result);
        }
    }

    public void setPayload(String payloadStr) {
        if (StringUtils.hasText(payloadStr)) {
            payload = JsonHelper.fromJSON(payloadStr, new TypeReference<List<OutputVariablesSettings>>() {});
        }
    }

}
