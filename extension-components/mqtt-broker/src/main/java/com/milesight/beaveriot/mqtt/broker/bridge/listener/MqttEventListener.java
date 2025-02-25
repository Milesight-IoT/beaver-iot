package com.milesight.beaveriot.mqtt.broker.bridge.listener;


public interface MqttEventListener {

    /**
     * Fire when a message is published to any topic. In cluster mode, this event will be fired only in one node.
     */
    default void onPublish(MqttMessageEvent event) {}

    /**
     * Fire when a message is published to any topic. In cluster mode, this event will be fired in all nodes.
     */
    default void onBroadcast(MqttMessageEvent event) {}

}
