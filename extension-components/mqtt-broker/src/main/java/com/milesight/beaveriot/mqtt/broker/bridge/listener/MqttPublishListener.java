package com.milesight.beaveriot.mqtt.broker.bridge.listener;


import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttMessageEvent;

@FunctionalInterface
public interface MqttPublishListener extends MqttEventListener {

    @Override
    void onPublish(MqttMessageEvent event);

}
