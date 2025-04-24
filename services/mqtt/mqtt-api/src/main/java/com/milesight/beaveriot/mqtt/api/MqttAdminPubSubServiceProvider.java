package com.milesight.beaveriot.mqtt.api;


import com.milesight.beaveriot.context.api.MqttPubSubServiceProvider;
import com.milesight.beaveriot.context.mqtt.enums.MqttQos;
import com.milesight.beaveriot.context.mqtt.enums.MqttTopicChannel;
import com.milesight.beaveriot.context.mqtt.listener.MqttMessageListener;

public interface MqttAdminPubSubServiceProvider extends MqttPubSubServiceProvider {

    void publish(String topicPrefix, String username, String topicSubPath, byte[] payload, MqttQos qos, boolean retained);

    /**
     * Publish message to mqtt broker
     */
    void publish(MqttTopicChannel mqttTopicChannel, String username, String topicSubPath, byte[] payload, MqttQos qos, boolean retained);

    void subscribe(String topicPrefix, String username, String topicSubPath, MqttMessageListener onMessage);

    void subscribe(String topicPrefix, String username, String topicSubPath, MqttMessageListener onMessage, boolean shared);

    /**
     * Subscribe messages matched with given topic information.
     * <p>
     * If shared is true and the cluster mode is enabled, the event will be fired only in one node.
     */
    void subscribe(MqttTopicChannel mqttTopicChannel, String username, String topicSubPath, MqttMessageListener onMessage, boolean shared);

    void subscribe(MqttTopicChannel mqttTopicChannel, String username, String topicSubPath, MqttMessageListener onMessage);

    void unsubscribe(String topicPrefix, String username, String topicSubPath);

    /**
     * Remove all listener matched with given topic information.
     */
    void unsubscribe(MqttTopicChannel mqttTopicChannel, String username, String topicSubPath);

    static String getWebUsername(String tenantId) {
        return String.format("web_mqtt@%s", tenantId);
    }

}
