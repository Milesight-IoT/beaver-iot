package com.milesight.beaveriot.rule.components.mqtt;

import io.moquette.broker.Server;
import lombok.*;
import lombok.extern.slf4j.*;
import org.apache.camel.BeanInject;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ExtendedStartupListener;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

import java.util.List;
import java.util.Map;

@Slf4j
@Component("simpleMqtt")
public class SimpleMqttComponent extends DefaultComponent implements ExtendedStartupListener {

    @Getter
    @BeanInject
    private Server mqttBroker;

    @BeanInject
    private List<MqttMessageListener> mqttMessageListeners;

    public SimpleMqttComponent(CamelContext camelContext) {
        super(camelContext);
    }

    public void subscribe(MqttMessageListener listener) {
        mqttMessageListeners.add(listener);
    }

    public void unsubscribe(MqttMessageListener listener) {
        mqttMessageListeners.remove(listener);
    }

    @Override
    public void onCamelContextStarted(CamelContext context, boolean alreadyStarted) {
        // do nothing
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        log.info("server: {}", mqttBroker);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        var endpoint = new SimpleMqttEndpoint(uri, remaining, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }
}
