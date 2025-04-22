package com.milesight.beaveriot.rule.components.httpin;

import com.milesight.beaveriot.rule.components.httpin.model.ListenConfig;
import com.milesight.beaveriot.rule.support.JsonHelper;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * HttpInConsumer class.
 *
 * @author simon
 * @date 2025/4/17
 */
@Slf4j
public class HttpInConsumer extends DefaultConsumer {
    HttpInEndpoint httpInEndpoint;

    public HttpInConsumer(HttpInEndpoint endpoint, Processor processor) {
        super(endpoint, processor);
        this.httpInEndpoint = endpoint;
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        ListenConfig config = new ListenConfig();
        config.setUrlTemplate(httpInEndpoint.getUrlTemplate());
        config.setMethod(httpInEndpoint.getMethod());
        config.setCredentialsId(httpInEndpoint.getCredentialsId());
        config.setCb(request -> {
            Exchange exchange = httpInEndpoint.createExchange();
            try {
                Map<String, Object> payload = new java.util.HashMap<>(Map.of(
                        HttpInConstants.OUT_HEADER_NAME, JsonHelper.toJSON(request.getHeaders()),
                        HttpInConstants.OUT_BODY_NAME, request.getBody(),
                        HttpInConstants.OUT_URL_NAME, request.getUrl()
                ));
                request.getPathParams().forEach((key, value) -> payload.put(HttpInConstants.OUT_PATH_PARAM_NAME + "." + key, value));
                exchange.getIn().setBody(payload);
                getProcessor().process(exchange);
            } catch (Exception e) {
                log.error("Process error: " + e.getMessage());
            }
        });
        httpInEndpoint
                .getComponent(HttpInComponent.class)
                .getHttpInRequestListener()
                .registerUrl(httpInEndpoint.getTenantId(), httpInEndpoint.getFlowId(), config);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        httpInEndpoint.getComponent(HttpInComponent.class).getHttpInRequestListener().unregisterUrl(httpInEndpoint.getTenantId(), httpInEndpoint.getFlowId());
    }
}
