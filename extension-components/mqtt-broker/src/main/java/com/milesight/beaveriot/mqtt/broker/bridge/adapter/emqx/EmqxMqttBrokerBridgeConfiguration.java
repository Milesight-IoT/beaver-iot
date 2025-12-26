package com.milesight.beaveriot.mqtt.broker.bridge.adapter.emqx;

import com.milesight.beaveriot.mqtt.broker.bridge.MqttBrokerSettings;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAuthProvider;
import lombok.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpClient;

@Configuration
@ConditionalOnProperty(prefix = "mqtt.broker", name = "type", havingValue = "emqx")
public class EmqxMqttBrokerBridgeConfiguration {

    @Bean("emqxHttpClient")
    public HttpClient emqxHttpClient(MqttBrokerSettings mqttBrokerSettings) {
        val username = mqttBrokerSettings.getEmqx().getRestApiUsername();
        val password = mqttBrokerSettings.getEmqx().getRestApiPassword();
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .authenticator(new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password.toCharArray());
                    }
                })
                .build();
    }

    @Bean
    public EmqxRestApi emqxRestApi(@Qualifier("emqxHttpClient") HttpClient httpClient, MqttBrokerSettings mqttBrokerSettings) {
        return new EmqxRestApi(mqttBrokerSettings.getEmqx().getRestApiEndpoint(), httpClient);
    }

    @Bean(name = "emqxMqttBrokerBridge", initMethod = "open", destroyMethod = "close")
    public EmqxMqttBrokerBridge emqxMqttBrokerBridge(EmqxRestApi emqxRestApi,
                                                     MqttAuthProvider mqttAuthProvider) {
        return new EmqxMqttBrokerBridge(mqttAuthProvider, emqxRestApi);
    }

}
