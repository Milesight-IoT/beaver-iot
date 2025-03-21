package com.milesight.beaveriot.mqtt.broker.bridge.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqttAuthConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MqttAuthProvider defaultMqttAuthProvider() {
        return new MqttAuthProvider() {
            @Override
            public boolean canDo(MqttAction action, String topic, String clientId, String username) {
                throw new UnsupportedOperationException("Missing MqttAuthProvider implementation.");
            }

            @Override
            public boolean canLogin(String clientId, String username, String password) {
                throw new UnsupportedOperationException("Missing MqttAuthProvider implementation.");
            }
        };
    }

}
