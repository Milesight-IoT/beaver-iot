package com.milesight.beaveriot.rule.components.mqtt;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.mqtt.enums.MqttTopicChannel;
import com.milesight.beaveriot.context.mqtt.listener.MqttMessageListener;
import io.moquette.broker.subscriptions.Topic;
import lombok.extern.slf4j.*;
import lombok.*;
import org.apache.camel.Endpoint;
import org.apache.camel.Processor;
import org.apache.camel.support.DefaultConsumer;

import java.util.Map;

@Slf4j
public class SimpleMqttConsumer extends DefaultConsumer {

    private MqttMessageListener listener;

    public SimpleMqttConsumer(Endpoint endpoint, Processor processor) {
        super(endpoint, processor);
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        unsubscribeListener();

        val component = getEndpoint().getComponent(SimpleMqttComponent.class);
        val credentials = component.getCredentialsServiceProvider().getCredentials(CredentialsType.MQTT.name())
                .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NO_FOUND, "credentials not found"));
        val username = credentials.getAccessKey();
        val topicSubPath = getEndpoint().getSubscriptionTopic()
                // Only specific topic path pattern allowed here.
                // So if it not starts with `beaver-iot/{username}` then treat the whole string as a sub-path
                .replaceFirst("^%s/%s".formatted(MqttTopicChannel.DEFAULT.getTopicPrefix(), username), "");
        val subscriptionTopic = Topic.asTopic(String.format("%s/%s/%s".formatted(MqttTopicChannel.DEFAULT.getTopicPrefix(), username, topicSubPath)));

        listener = message -> {
            val topic = message.getFullTopicName();
            val encoding = MqttPayloadEncodingType.fromString(getEndpoint().getEncoding());
            val payload = encoding.encode(message.getPayload());
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

        component.getMqttPubSubServiceProvider().subscribe(username, topicSubPath, listener);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        unsubscribeListener();
    }

    private void unsubscribeListener() {
        if (listener == null) {
            return;
        }
        getEndpoint().getComponent(SimpleMqttComponent.class).getMqttPubSubServiceProvider().unsubscribe(listener);
    }

    @Override
    public SimpleMqttEndpoint getEndpoint() {
        return (SimpleMqttEndpoint) super.getEndpoint();
    }

}
