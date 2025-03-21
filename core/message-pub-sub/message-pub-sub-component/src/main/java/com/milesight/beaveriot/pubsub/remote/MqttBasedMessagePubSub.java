package com.milesight.beaveriot.pubsub.remote;

import com.milesight.beaveriot.mqtt.broker.bridge.MqttBrokerBridge;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.MqttBroadcastListener;
import com.milesight.beaveriot.pubsub.MessagePubSub;
import com.milesight.beaveriot.pubsub.MessageRouter;
import io.netty.handler.codec.mqtt.MqttQoS;
import jakarta.annotation.PostConstruct;

public class MqttBasedMessagePubSub extends MessagePubSub {

    public static final String INTERNAL_TOPIC_PREFIX = "beaver-iot-internal";

    public static final String INTERNAL_USERNAME = "_";

    public static final String INTERNAL_BROADCAST_TOPIC = String.format("%s/%s/%s", INTERNAL_TOPIC_PREFIX, INTERNAL_USERNAME, "broadcast");

    private final MqttBrokerBridge mqttBrokerBridge;

    public MqttBasedMessagePubSub(MqttBrokerBridge mqttBrokerBridge, MessageRouter messageRouter) {
        super(messageRouter);
        this.mqttBrokerBridge = mqttBrokerBridge;
    }

    @Override
    protected void remoteBroadcast(String message) {
        mqttBrokerBridge.publish(INTERNAL_BROADCAST_TOPIC, message.getBytes(), MqttQoS.AT_LEAST_ONCE, true);
    }

    @PostConstruct
    private void init() {
        mqttBrokerBridge.addListener((MqttBroadcastListener) event -> {
            if (event.getTopic().startsWith(INTERNAL_BROADCAST_TOPIC)) {
                onRemoteBroadcastMessage(new String(event.getPayload()));
            }
        });
    }

}
