package com.milesight.beaveriot.rule.components.httpin;

import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.BeanInject;
import org.apache.camel.Endpoint;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;

/**
 * HttpInComponent class.
 *
 * @author simon
 * @date 2025/4/17
 */
@Slf4j
@Component("httpIn")
@Getter
public class HttpInComponent extends DefaultComponent {
    @BeanInject
    HttpInRequestListener httpInRequestListener;

    @BeanInject
    CredentialsServiceProvider credentialsServiceProvider;

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        HttpInEndpoint endpoint = new HttpInEndpoint(uri, remaining, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }
}
