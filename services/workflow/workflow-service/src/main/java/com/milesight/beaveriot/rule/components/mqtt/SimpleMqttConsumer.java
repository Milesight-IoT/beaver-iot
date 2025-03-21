package com.milesight.beaveriot.rule.components.mqtt;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.context.mqtt.MqttMessageListener;
import com.milesight.beaveriot.context.mqtt.MqttTopicChannel;
import com.milesight.beaveriot.context.security.TenantContext;
import io.moquette.broker.subscriptions.Topic;
import lombok.extern.slf4j.*;
import lombok.*;
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
    }

    @Override
    protected void doStart() throws Exception {
        super.doStart();
        val component = getEndpoint().getComponent(SimpleMqttComponent.class);

        val tenantId = TenantContext.getTenantId();
        Credentials credentials;
        if (getEndpoint().getCredentialsId() != null) {
            try {
                long credentialsId = Long.parseLong(getEndpoint().getCredentialsId());
                credentials = component.getCredentialsServiceFacade().getCredentials(tenantId, credentialsId)
                        .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NO_FOUND, "credentials not found"));
            } catch (NumberFormatException e) {
                throw new ServiceException(ErrorCode.PARAMETER_VALIDATION_FAILED, "credentialsId should be in a numeric format");
            }
        } else {
            credentials = component.getCredentialsServiceFacade().getCredentials(tenantId, "MQTT")
                    .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NO_FOUND, "credentials not found"));
        }


        val username = credentials.getAccessKey();
        val topicSubPath = getEndpoint().getSubscriptionTopic()
                // Only specific topic path pattern allowed here.
                // So if it not starts with `beaver-iot/{username}` then treat the whole string as a sub-path
                .replaceFirst("^%s/%s".formatted(MqttTopicChannel.DEFAULT.getTopicPrefix(), username), "");
        component.getMqttPubSubServiceProvider().subscribe(username, topicSubPath, listener);
    }

    @Override
    protected void doStop() throws Exception {
        super.doStop();
        getEndpoint().getComponent(SimpleMqttComponent.class).getMqttPubSubServiceProvider().unsubscribe(listener);
    }

    @Override
    public SimpleMqttEndpoint getEndpoint() {
        return (SimpleMqttEndpoint) super.getEndpoint();
    }

}
