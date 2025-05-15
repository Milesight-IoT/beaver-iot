package com.milesight.beaveriot.rule.components.serviceinvocation;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.util.ExchangeContextHelper;
import com.milesight.beaveriot.eventbus.api.EventResponse;
import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.ExchangeHeaders;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.model.OutputVariablesSettings;
import com.milesight.beaveriot.rule.support.JsonHelper;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import com.milesight.beaveriot.rule.util.WorkflowEntityHelper;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.spi.UriParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author loong
 * @date 2024/12/23 11:34
 */
@RuleNode(value = "serviceInvocation", type = RuleNodeType.ACTION, description = "Service Invocation", testable = false)
@Data
public class ServiceInvocationComponent implements ProcessorNode<Exchange> {

    @UriParam(javaType = "java.util.Map", prefix = "bean", displayName = "Service Setting")
    @UriParamExtension(uiComponent = "serviceEntitySetting", loggable = true)
    private Map<String, Object> serviceInvocationSetting;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    WorkflowEntityHelper workflowEntityHelper;

    @UriParamExtension(uiComponent = "paramDefineInput")
    @OutputArguments
    @UriParam(prefix = "bean", name = "payload", displayName = "Output Variables",description = "Define the response of the service call, which is optional and should only be set when the service has a return value.")
    private List<OutputVariablesSettings> payload;

    @Override
    public void processor(Exchange exchange) {
        if(serviceInvocationSetting != null && serviceInvocationSetting.get("serviceParams") != null) {
            Map<String, Object> serviceParams = JsonHelper.fromJSON(JsonHelper.toJSON(serviceInvocationSetting.get("serviceParams")), Map.class);
            Map<String, Object> exchangePayloadVariables = SpELExpressionHelper.resolveExpression(exchange, serviceParams);
            workflowEntityHelper.checkEntityExist(exchangePayloadVariables.keySet());
            ExchangePayload exchangePayload = ExchangePayload.create(exchangePayloadVariables);
            if (ObjectUtils.isEmpty(exchange.getProperty(ExchangeHeaders.EXCHANGE_ROOT_FLOW_ID))) {
                exchange.setProperty(ExchangeHeaders.EXCHANGE_ROOT_FLOW_ID, exchange.getFromRouteId());
            }
            ExchangeContextHelper.initializeExchangeContext(exchangePayload, exchange);

            EventResponse eventResponse = entityValueServiceProvider.saveValuesAndPublishSync(exchangePayload);

            OutputVariablesSettings.validate(eventResponse, payload);

            exchange.getIn().setBody(eventResponse);
        }
    }

    public void setPayload(String payloadStr) {
        if (StringUtils.hasText(payloadStr)) {
            payload = JsonHelper.fromJSON(payloadStr, new TypeReference<List<OutputVariablesSettings>>() {});
        }
    }

}
