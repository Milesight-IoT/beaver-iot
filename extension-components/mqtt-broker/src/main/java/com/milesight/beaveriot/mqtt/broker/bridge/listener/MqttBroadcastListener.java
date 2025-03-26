package com.milesight.beaveriot.mqtt.broker.bridge.listener;


import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttMessageEvent;

@FunctionalInterface
public interface MqttBroadcastListener extends MqttEventListener {

    @Override
    void onBroadcast(MqttMessageEvent event);

}
