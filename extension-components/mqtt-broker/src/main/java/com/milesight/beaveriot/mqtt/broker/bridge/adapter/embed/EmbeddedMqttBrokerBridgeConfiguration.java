package com.milesight.beaveriot.mqtt.broker.bridge.adapter.embed;

import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAuthProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;


@Configuration
@ConditionalOnProperty(prefix = "cluster", name = "enabled", havingValue = "false", matchIfMissing = true)
public class EmbeddedMqttBrokerBridgeConfiguration {

    @Bean("embeddedBrokerProperties")
    @ConfigurationProperties(prefix = "mqtt.broker.moquette")
    public Properties embeddedBrokerProperties() {
        return new Properties();
    }


    @Bean(name = "embeddedMqttBrokerBridge", initMethod = "open", destroyMethod = "close")
    public EmbeddedMqttBrokerBridge embeddedMqttBrokerBridge(MqttAuthProvider mqttAuthProvider,
                                                              @Qualifier("embeddedBrokerProperties") Properties embeddedBrokerProperties) {
        return new EmbeddedMqttBrokerBridge(mqttAuthProvider, embeddedBrokerProperties);
    }

}
