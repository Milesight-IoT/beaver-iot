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
import org.apache.camel.http.base.HttpOperationFailedException;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriParam;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.Assert;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author loong
 */
@RuleNode(value = "httpRequest", type = RuleNodeType.ACTION, description = "HttpRequest")
@Data
public class HttpRequestComponent implements ProcessorNode<Exchange> {

    @UriParam(javaType = "string", prefix = "bean", displayName = "API/API Method", enums = "GET,POST,PUT,DELETE")
    @UriParamExtension(uiComponentGroup = "API")
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
    @UriParamExtension(uiComponent = "paramDefineInput", initialValue = "[{\"name\":\"statusCode\",\"type\":\"LONG\"},{\"name\":\"responseBody\",\"type\":\"STRING\"},{\"name\":\"responseHeaders\",\"type\":\"STRING\"}]")
    @UriParam(displayName = "Output Variables",prefix = "bean", description = "Received HTTP message.")
    @Metadata(required = true, autowired = true)
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
            String queryString = toQueryString(paramsVariables, true);
            requestUrl = requestUrl.indexOf("?") == -1 ? requestUrl.append("?").append(queryString) : requestUrl.append("&").append(queryString);
        }

        Object bodyValueVariables = null;
        if (body != null) {
            String bodyType = body.get("type") == null ? null : body.get("type").toString();
            if (bodyType != null) {
                httpHeader.put(Exchange.CONTENT_TYPE, bodyType + ";charset=UTF-8");
            }
            bodyValueVariables = convertBody(exchange, body.get("value"), bodyType);
        }
        Object finalBodyValueVariables = bodyValueVariables;
        Exchange responseExchange = producerTemplate.request(requestUrl.toString(), exchange1 -> {
            exchange1.getIn().setHeaders(httpHeader);
            exchange1.getIn().setBody(finalBodyValueVariables);
        });
        if (responseExchange != null) {
            if (responseExchange.getException() == null) {
                int statusCode = responseExchange.getMessage().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);
                String responseBody = responseExchange.getMessage().getBody(String.class);
                Map<String, Object> responseHeaders = responseExchange.getMessage().getHeaders();

                createExchangeBody(exchange, statusCode, responseBody, JsonHelper.toJSON(responseHeaders));
            } else {
                if (responseExchange.getException() instanceof HttpOperationFailedException httpOperationFailedException) {
                    createExchangeBody(exchange, httpOperationFailedException.getStatusCode(), httpOperationFailedException.getResponseBody(),
                            JsonHelper.toJSON(httpOperationFailedException.getResponseHeaders()));
                } else {
                    exchange.setException(responseExchange.getException());
                }
            }
        }
    }

    private String toQueryString(Map<String, Object> paramsVariables, boolean encodeValue) {
        if (ObjectUtils.isEmpty(paramsVariables)) {
            return null;
        }
        return paramsVariables.entrySet().stream()
                .map(entry -> {
                    if (encodeValue) {
                        return entry.getKey() + "=" + URLEncoder.encode(ObjectUtils.toString(entry.getValue()), StandardCharsets.UTF_8);
                    } else {
                        return entry.getKey() + "=" + ObjectUtils.toString(entry.getValue());
                    }
                })
                .collect(Collectors.joining("&"));
    }

    private Object convertBody(Exchange exchange, Object bodyValue, String contentType) {
        if (ObjectUtils.isEmpty(bodyValue)) {
            return null;
        }

        if (MediaType.APPLICATION_FORM_URLENCODED_VALUE.equals(contentType)) {
            Assert.isTrue(bodyValue instanceof Map<?,?> , "bodyValue must be a Map when contentType is application/x-www-form-urlencoded");
            return SpELExpressionHelper.resolveStringExpression(exchange, toQueryString((Map<String, Object>) bodyValue, false));
        } else {
            return SpELExpressionHelper.resolveStringExpression(exchange, bodyValue);
        }
    }

    private void createExchangeBody(Exchange exchange, int statusCode, String responseBody, String responseHeaders) {
        Map<String, Object> bodyOut = new HashMap<>();
        bodyOut.put("statusCode", statusCode);
        bodyOut.put("responseBody", responseBody);
        bodyOut.put("responseHeaders", responseHeaders);
        exchange.getIn().setBody(bodyOut);
    }
}
