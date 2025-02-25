package com.milesight.beaveriot.mqtt.broker.bridge.listener;


@FunctionalInterface
public interface MqttBroadcastListener extends MqttEventListener {

    @Override
    void onBroadcast(MqttMessageEvent event);

}
