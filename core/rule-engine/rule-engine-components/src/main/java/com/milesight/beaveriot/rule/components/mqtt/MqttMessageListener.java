package com.milesight.beaveriot.rule.components.mqtt;

@FunctionalInterface
public interface MqttMessageListener {

    void onMessage(String topic, String message);

}
