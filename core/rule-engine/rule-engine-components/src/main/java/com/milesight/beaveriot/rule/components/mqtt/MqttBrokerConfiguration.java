package com.milesight.beaveriot.rule.components.mqtt;

import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptPublishMessage;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Configuration
public class MqttBrokerConfiguration {

    @Bean
    public List<MqttMessageListener> mqttMessageListeners() {
        return new CopyOnWriteArrayList<>();
    }

    @Bean("moquetteProperties")
    @ConfigurationProperties(prefix = "moquette")
    public Properties moquetteProperties() {
        return new Properties();
    }

    @Bean(destroyMethod = "stopServer")
    public Server mqttBroker(@Qualifier("moquetteProperties") Properties moquetteProperties, List<MqttMessageListener> mqttMessageListeners) throws IOException {
        var mqttBroker = new Server();
        mqttBroker.startServer(new MemoryConfig(moquetteProperties), List.of(new AbstractInterceptHandler() {

            @Override
            public void onPublish(InterceptPublishMessage msg) {
                log.debug("Received on topic: '" + msg.getTopicName() + "' content: '" + msg.getPayload().toString(StandardCharsets.UTF_8) + "'");
                mqttMessageListeners.forEach(listener -> {
                    try {
                        listener.onMessage(msg.getTopicName(), msg.getPayload().toString(StandardCharsets.UTF_8));
                    } catch (Exception e) {
                        log.error("Handle mqtt message failed.", e);
                    }
                });

                // release message buffer here
                super.onPublish(msg);
            }

            @Override
            public String getID() {
                return "DefaultListener";
            }

            @Override
            public void onSessionLoopError(Throwable throwable) {
                log.error("session loop error", throwable);
            }
        }));
        return mqttBroker;
    }

}
