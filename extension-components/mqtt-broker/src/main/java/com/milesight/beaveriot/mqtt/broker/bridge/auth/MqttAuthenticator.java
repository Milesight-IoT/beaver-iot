package com.milesight.beaveriot.mqtt.broker.bridge.auth;

public interface MqttAuthenticator {

    boolean canLogin(String clientId, String username, String password);

}
