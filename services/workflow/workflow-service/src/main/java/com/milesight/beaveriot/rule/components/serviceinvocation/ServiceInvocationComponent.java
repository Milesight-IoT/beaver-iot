package com.milesight.beaveriot.rule.components.serviceinvocation;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.ExchangeFlowExecutor;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.util.ExchangeContextHelper;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.spi.UriParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

/**
 * @author loong
 * @date 2024/12/23 11:34
 */
@RuleNode(value = "serviceInvocation", type = RuleNodeType.ACTION, description = "Service Invocation")
@Data
public class ServiceInvocationComponent implements ProcessorNode<Exchange> {

    @UriParam(javaType = "java.util.Map", prefix = "bean")
    @UriParamExtension(uiComponent = "ServiceEntitySetting")
    private Map<String, Object> serviceParams;

    @Autowired
    EntityValueServiceProvider entityValueServiceProvider;
    @Autowired
    private ExchangeFlowExecutor exchangeFlowExecutor;

    @Override
    public void processor(Exchange exchange) {
        Map<String, Object> exchangePayloadVariables = SpELExpressionHelper.resolveExpression(exchange, serviceParams);
        ExchangePayload payload = ExchangePayload.create(exchangePayloadVariables);

        exchange.getIn().setBody(payload);

        ExchangeContextHelper.initializeEventSource(payload, exchange);

        exchangeFlowExecutor.syncExchange(payload);
    }
}
