package com.milesight.beaveriot.mqtt.broker.bridge.auth;


public interface MqttAclValidator {

    boolean canDo(MqttAction action, String topic, String clientId, String username);

}
