package com.milesight.beaveriot.mqtt.broker.bridge.listener;


@FunctionalInterface
public interface MqttPublishListener extends MqttEventListener {

    @Override
    void onPublish(MqttMessageEvent event);

}
