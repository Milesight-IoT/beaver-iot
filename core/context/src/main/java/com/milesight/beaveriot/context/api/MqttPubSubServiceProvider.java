package com.milesight.beaveriot.context.api;


import com.milesight.beaveriot.context.mqtt.enums.MqttQos;
import com.milesight.beaveriot.context.mqtt.listener.MqttConnectEventListener;
import com.milesight.beaveriot.context.mqtt.listener.MqttDisconnectEventListener;
import com.milesight.beaveriot.context.mqtt.listener.MqttMessageListener;
import com.milesight.beaveriot.context.mqtt.listener.MqttPubSubServiceListener;
import com.milesight.beaveriot.context.mqtt.model.MqttBrokerInfo;

public interface MqttPubSubServiceProvider {

    /**
     * Publish message to mqtt topic 'beaver-iot/${username}/${topicSubPath}'
     */
    void publish(String username, String topicSubPath, byte[] payload, MqttQos qos, boolean retained);

    /**
     * Publish message to mqtt topic 'beaver-iot/mqtt@${tenantId}/${topicSubPath}'
     */
    void publish(String topicSubPath, byte[] payload, MqttQos qos, boolean retained);

    /**
     * Subscribe mqtt messages from the topic matches 'beaver-iot/${username}/${topicSubPath}'.
     * <p>
     * If `shared` is true and the cluster mode is enabled, the event will be fired only in one node. (Perform like mqtt shared subscription, but work on cluster level)
     */
    void subscribe(String username, String topicSubPath, MqttMessageListener onMessage, boolean shared);

    /**
     * Subscribe mqtt messages from the topic matches 'beaver-iot/+/${topicSubPath}'.
     * <p>
     * If `shared` is true and the cluster mode is enabled, the event will be fired only in one node. (Perform like mqtt shared subscription, but work on cluster level)
     */
    void subscribe(String topicSubPath, MqttMessageListener onMessage, boolean shared);

    /**
     * Subscribe mqtt messages from the topic matches 'beaver-iot/${username}/${topicSubPath}'.
     * <p>
     * If `shared` is true and the cluster mode is enabled, the event will be fired only in one node. (Perform like mqtt shared subscription, but work on cluster level)
     */
    void subscribe(String username, String topicSubPath, MqttMessageListener onMessage);

    /**
     * Subscribe mqtt messages from the topic matches 'beaver-iot/+/${topicSubPath}'.
     */
    void subscribe(String topicSubPath, MqttMessageListener onMessage);

    /**
     * Remove given listener.
     */
    void unsubscribe(MqttPubSubServiceListener onMessage);

    /**
     * Remove all listeners which subscribe the topic 'beaver-iot/${username}/${topicSubPath}'.
     */
    void unsubscribe(String username, String topicSubPath);

    /**
     * Remove all listeners which subscribe the topic 'beaver-iot/+/${topicSubPath}'.
     */
    void unsubscribe(String topicSubPath);

    void onConnect(MqttConnectEventListener listener);

    void onDisconnect(MqttDisconnectEventListener listener);

    /**
     * Get full topic name
     * @return 'beaver-iot/${username}/${topicSubPath}'
     */
    String getFullTopicName(String username, String topicSubPath);

    /**
     * Get full topic name
     * @return 'beaver-iot/+/${topicSubPath}'
     */
    String getFullTopicName(String topicSubPath);

    /**
     * Get mqtt broker info
     */
    MqttBrokerInfo getMqttBrokerInfo();

}
