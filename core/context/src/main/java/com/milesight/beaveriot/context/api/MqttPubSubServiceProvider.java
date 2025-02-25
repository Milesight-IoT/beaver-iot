package com.milesight.beaveriot.context.api;


import com.milesight.beaveriot.context.mqtt.MqttBrokerInfo;
import com.milesight.beaveriot.context.mqtt.MqttMessageListener;
import com.milesight.beaveriot.context.mqtt.MqttQos;

public interface MqttPubSubServiceProvider {

    /**
     * Publish message to mqtt broker
     */
    void publish(String username, String topicSubPath, byte[] payload, MqttQos qos, boolean retained);

    /**
     * Subscribe messages matched with given topic information.
     * <p>
     * If `shared` is true and the cluster mode is enabled, the event will be fired only in one node. (Perform like mqtt shared subscription, but work on cluster level)
     */
    void subscribe(String username, String topicSubPath, MqttMessageListener onMessage, boolean shared);

    void subscribe(String username, String topicSubPath, MqttMessageListener onMessage);

    /**
     * Remove given listener.
     */
    void unsubscribe(MqttMessageListener onMessage);

    /**
     * Remove all listener matched with given topic information.
     */
    void unsubscribe(String username, String topicSubPath);

    String getFullTopicName(String username, String topicSubPath);

    MqttBrokerInfo getMqttBrokerInfo();

}
