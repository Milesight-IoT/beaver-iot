package com.milesight.beaveriot.rule.components.httprequest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.model.OutputVariablesSettings;
import com.milesight.beaveriot.rule.support.JsonHelper;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.UriParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author loong
 */
@RuleNode(value = "httpRequest", type = RuleNodeType.ACTION, description = "HttpRequest")
@Data
public class HttpRequestComponent implements ProcessorNode<Exchange> {

    @UriParam(javaType = "string", prefix = "bean", displayName = "API/API Method")
    @UriParamExtension(uiComponentGroup = "API", uiComponent = "method")
    private String method;
    @UriParam(javaType = "string", prefix = "bean", displayName = "URL")
    @UriParamExtension(uiComponentGroup = "API", uiComponent = "url")
    private String url;
    @UriParam(javaType = "java.util.Map", prefix = "bean", displayName = "Header")
    @UriParamExtension(uiComponent = "paramAssignInput", loggable = true)
    private Map<String, Object> header;
    @UriParam(javaType = "java.util.Map", prefix = "bean", displayName = "PARAMS")
    @UriParamExtension(uiComponent = "paramAssignInput", loggable = true)
    private Map<String, Object> params;
    @UriParam(javaType = "java.util.Map", prefix = "bean", displayName = "Body")
    @UriParamExtension(uiComponent = "httpBodyInput", loggable = true)
    private Map<String, Object> body;

    @OutputArguments(displayName = "Output Variables")
    @UriParamExtension(uiComponent = "paramDefineInput")
    @UriParam(displayName = "Output Variables", description = "Received HTTP message.", defaultValue = "[{\"name\":\"statusCode\",\"type\":\"LONG\"},{\"name\":\"responseBody\",\"type\":\"STRING\"},{\"name\":\"responseHeaders\",\"type\":\"STRING\"}]")
    private List<OutputVariablesSettings> message;

    @Autowired
    private ProducerTemplate producerTemplate;

    public void setMessage(String json) {
        //noinspection Convert2Diamond
        message = JsonHelper.fromJSON(json, new TypeReference<List<OutputVariablesSettings>>() {
        });
    }

    @Override
    public void processor(Exchange exchange) {
        Map<String, Object> httpHeader = new HashMap<>();
        httpHeader.put(Exchange.HTTP_METHOD, method);
        if (header != null && !header.isEmpty()) {
            Map<String, Object> headerVariables = SpELExpressionHelper.resolveExpression(exchange, header);
            httpHeader.putAll(headerVariables);
        }
        StringBuilder requestUrl = new StringBuilder(url);
        if (params != null && !params.isEmpty()) {
            Map<String, Object> paramsVariables = SpELExpressionHelper.resolveExpression(exchange, params);
            paramsVariables.forEach((key, value) -> {
                if (requestUrl.indexOf("?") == -1) {
                    requestUrl.append("?");
                } else {
                    requestUrl.append("&");
                }
                requestUrl.append(key).append("=").append(value);
            });
        }
        Object bodyValueVariables = null;
        if (body != null) {
            String bodyType = body.get("type") == null ? null : body.get("type").toString();
            if (bodyType != null) {
                httpHeader.put(Exchange.CONTENT_TYPE, bodyType);
            }
            Object bodyValue = body.get("value");
            if (bodyValue != null) {
                bodyValueVariables = SpELExpressionHelper.resolveStringExpression(exchange, bodyValue);
            }
        }
        Object finalBodyValueVariables = bodyValueVariables;
        Exchange responseExchange = producerTemplate.request(requestUrl.toString(), exchange1 -> {
            exchange1.getIn().setHeaders(httpHeader);
            exchange1.getIn().setBody(finalBodyValueVariables);
        });
        if (responseExchange != null) {
            Map<String, Object> bodyOut = new HashMap<>();

            int statusCode = responseExchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
            String responseBody = responseExchange.getMessage().getBody(String.class);
            Map<String, Object> responseHeaders = responseExchange.getMessage().getHeaders();

            bodyOut.put("statusCode", statusCode);
            bodyOut.put("responseBody", responseBody);
            bodyOut.put("responseHeaders", JsonHelper.toJSON(responseHeaders));
            exchange.getIn().setBody(bodyOut);
        }
    }

}
