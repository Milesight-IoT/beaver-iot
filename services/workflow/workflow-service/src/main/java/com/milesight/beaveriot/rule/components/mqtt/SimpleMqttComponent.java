package com.milesight.beaveriot.rule.components.mqtt;

import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
import lombok.*;
import lombok.extern.slf4j.*;
import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ExtendedStartupListener;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;

@Slf4j
@Component("simpleMqtt")
public class SimpleMqttComponent extends DefaultComponent implements ExtendedStartupListener {

    @Getter
    @BeanInject
    private MqttPubSubServiceProvider mqttPubSubServiceProvider;

    @Getter
    @BeanInject
    private CredentialsServiceProvider credentialsServiceProvider;

    public SimpleMqttComponent(CamelContext camelContext) {
        super(camelContext);
    }

    @Override
    public void onCamelContextStarted(CamelContext context, boolean alreadyStarted) {
        // do nothing
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        var endpoint = new SimpleMqttEndpoint(uri, remaining, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }
}
