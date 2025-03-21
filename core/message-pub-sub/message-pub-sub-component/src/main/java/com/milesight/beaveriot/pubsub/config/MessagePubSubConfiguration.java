package com.milesight.beaveriot.pubsub.config;

import com.milesight.beaveriot.mqtt.broker.bridge.MqttBrokerBridge;
import com.milesight.beaveriot.pubsub.MessageRouter;
import com.milesight.beaveriot.pubsub.remote.MqttBasedMessagePubSub;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MessagePubSubConfiguration {

    @Bean
    public MqttBasedMessagePubSub messagePubSub(MqttBrokerBridge mqttBrokerBridge, MessageRouter messageRouter) {
        return new MqttBasedMessagePubSub(mqttBrokerBridge, messageRouter);
    }

}
