package com.milesight.beaveriot.rule.components.httprequest;

import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.UriParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * @author loong
 */
@Data
public class HttpRequestComponent implements ProcessorNode<Exchange> {

    @OutputArguments
    @UriParam(javaType = "string", prefix = "bean", displayName = "API/API Method")
    @UriParamExtension(uiComponent = "method")
    private String method;
    @OutputArguments
    @UriParam(javaType = "string", prefix = "bean", displayName = "URL")
    @UriParamExtension(uiComponent = "url")
    private String url;
    @OutputArguments
    @UriParam(javaType = "java.util.Map", prefix = "bean", displayName = "Header")
    @UriParamExtension(uiComponent = "header")
    private Map<String, Object> header;
    @OutputArguments
    @UriParam(javaType = "java.util.Map", prefix = "bean", displayName = "PARAMS")
    @UriParamExtension(uiComponent = "params")
    private Map<String, Object> params;
    @OutputArguments
    @UriParam(javaType = "string", prefix = "bean", displayName = "Data Encoding Format")
    @UriParamExtension(uiComponent = "bodyType")
    private String bodyType;
    @OutputArguments
    @UriParam(prefix = "bean", displayName = "Body")
    @UriParamExtension(uiComponent = "body")
    private Object body;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Override
    public void processor(Exchange exchange) {
        Map<String, Object> httpHeader = new HashMap<>();
        httpHeader.put(Exchange.HTTP_METHOD, method);
        if (header != null) {
            httpHeader.putAll(header);
        }
        StringBuilder requestUrl = new StringBuilder(url);
        if (params != null) {
            params.forEach((key, value) -> {
                if (requestUrl.indexOf("?") == -1) {
                    requestUrl.append("?");
                }else {
                    requestUrl.append("&");
                }
                requestUrl.append(key).append("=").append(value);
            });
        }

        httpHeader.put(Exchange.CONTENT_TYPE, bodyType);

        Exchange responseExchange = (Exchange) producerTemplate.requestBodyAndHeaders(
                requestUrl.toString(),
                body,
                httpHeader);
        if (responseExchange != null && responseExchange.getOut() != null) {
            Map<String, Object> responseHeaders = responseExchange.getOut().getHeaders();
            String responseBody = responseExchange.getOut().getBody(String.class);
            int statusCode = responseExchange.getOut().getHeader(Exchange.HTTP_RESPONSE_CODE, Integer.class);

            exchange.getIn().setBody(responseBody);
            exchange.getIn().setHeaders(responseHeaders);
            exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, statusCode);
        }
    }
}
