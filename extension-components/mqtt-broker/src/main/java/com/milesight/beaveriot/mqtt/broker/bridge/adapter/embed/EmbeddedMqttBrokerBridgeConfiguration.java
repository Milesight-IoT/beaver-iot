package com.milesight.beaveriot.mqtt.broker.bridge.adapter.embed;

import com.milesight.beaveriot.mqtt.broker.bridge.MqttBrokerSettings;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAuthProvider;
import lombok.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;


@Configuration
@ConditionalOnProperty(prefix = "mqtt.broker", name = "type", havingValue = "moquette", matchIfMissing = true)
public class EmbeddedMqttBrokerBridgeConfiguration {

    @Bean("embeddedBrokerProperties")
    @ConfigurationProperties(prefix = "mqtt.broker.moquette")
    public Properties embeddedBrokerProperties(MqttBrokerSettings mqttBrokerSettings) {
        val properties = new Properties();
        // Default settings
        if (mqttBrokerSettings.getHost() != null) {
            properties.setProperty("host", mqttBrokerSettings.getHost());
        }
        if (mqttBrokerSettings.getMqttPort() != null) {
            properties.setProperty("port", String.valueOf(mqttBrokerSettings.getMqttPort()));
        }
        if (mqttBrokerSettings.getWsPort() != null) {
            properties.setProperty("websocket_port", String.valueOf(mqttBrokerSettings.getWsPort()));
        }
        if (mqttBrokerSettings.getWsPath() != null) {
            properties.setProperty("websocket_path", mqttBrokerSettings.getWsPath());
        }
        properties.setProperty("allow_anonymous", "false");
        properties.setProperty("persistence_enabled", "false");
        properties.setProperty("netty.mqtt.message_size", "1048576");
        return properties;
    }


    @Bean(name = "embeddedMqttBrokerBridge", initMethod = "open", destroyMethod = "close")
    public EmbeddedMqttBrokerBridge embeddedMqttBrokerBridge(MqttAuthProvider mqttAuthProvider,
                                                              @Qualifier("embeddedBrokerProperties") Properties embeddedBrokerProperties) {
        return new EmbeddedMqttBrokerBridge(mqttAuthProvider, embeddedBrokerProperties);
    }

}
