package com.milesight.beaveriot.mqtt.broker.bridge;


import com.milesight.beaveriot.context.mqtt.model.MqttBrokerInfo;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAcl;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.MqttEventListener;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.io.IOException;

public interface MqttBrokerBridge {

    MqttBrokerInfo getBrokerInfo();

    void open() throws IOException;

    void close();

    void publish(String topic, byte[] payload, MqttQoS qos, boolean retained);

    void addListener(MqttEventListener listener);

    void removeListener(MqttEventListener listener);

    void addUser(String username, String password);

    void deleteUser(String username);

    void addAcl(MqttAcl acl);

    void deleteAcl(String username);

}
