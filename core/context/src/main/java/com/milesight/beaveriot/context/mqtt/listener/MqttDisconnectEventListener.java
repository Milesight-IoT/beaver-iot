package com.milesight.beaveriot.context.mqtt.listener;

import com.milesight.beaveriot.context.mqtt.model.MqttDisconnectEvent;

import java.util.function.Consumer;

@FunctionalInterface
public interface MqttDisconnectEventListener extends Consumer<MqttDisconnectEvent>, MqttPubSubServiceListener {

}
