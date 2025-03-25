package com.milesight.beaveriot.context.mqtt.listener;

import com.milesight.beaveriot.context.mqtt.model.MqttMessage;

import java.util.function.Consumer;

@FunctionalInterface
public interface MqttMessageListener extends Consumer<MqttMessage>, MqttPubSubServiceListener {

}
