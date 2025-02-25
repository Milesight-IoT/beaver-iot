package com.milesight.beaveriot.rule.components.mqtt;

import io.moquette.broker.subscriptions.Topic;
import lombok.extern.slf4j.*;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

import java.util.Map;

@Slf4j
public class SimpleMqttConsumer extends DefaultConsumer {

    private final MqttMessageListener listener;

    private final Topic subscriptionTopic;

    public SimpleMqttConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
        subscriptionTopic = Topic.asTopic(getEndpoint().getSubscriptionTopic());
        listener = (topic, payload) -> {
            if (Topic.asTopic(topic).match(subscriptionTopic)) {
                log.debug("Matched subscription: '" + subscriptionTopic + "'");
                try {
                    var exchange = getEndpoint().createExchange();
                    exchange.getIn().setBody(Map.of("topic", topic, "payload", payload));
                    getProcessor().process(exchange);
                } catch (Exception e) {
                    log.error("handle mqtt payload failed", e);
                }
            }
        };
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        getEndpoint().getComponent(SimpleMqttComponent.class).subscribe(listener);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        getEndpoint().getComponent(SimpleMqttComponent.class).unsubscribe(listener);
    }

    @Override
    public SimpleMqttEndpoint getEndpoint() {
        return (SimpleMqttEndpoint) super.getEndpoint();
    }

}
