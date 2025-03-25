package com.milesight.beaveriot.context.mqtt.listener;

import com.milesight.beaveriot.context.mqtt.model.MqttConnectEvent;

import java.util.function.Consumer;

@FunctionalInterface
public interface MqttConnectEventListener extends Consumer<MqttConnectEvent>, MqttPubSubServiceListener {

}
