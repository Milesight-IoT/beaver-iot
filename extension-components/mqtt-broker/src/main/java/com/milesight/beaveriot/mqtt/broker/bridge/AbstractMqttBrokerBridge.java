package com.milesight.beaveriot.mqtt.broker.bridge;

import com.milesight.beaveriot.context.mqtt.model.MqttBrokerInfo;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAuthProvider;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.MqttEventListener;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttClientConnectEvent;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttClientDisconnectEvent;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttMessageEvent;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Slf4j
public abstract class AbstractMqttBrokerBridge implements MqttBrokerBridge {

    private final List<MqttEventListener> listeners = new CopyOnWriteArrayList<>();

    @Getter
    private final MqttAuthProvider mqttAuthProvider;

    @Autowired
    protected MqttBrokerSettings mqttBrokerSettings;

    @Qualifier("mqtt")
    @Autowired
    private TaskExecutor executor;

    protected AbstractMqttBrokerBridge(MqttAuthProvider mqttAuthProvider) {
        this.mqttAuthProvider = mqttAuthProvider;
    }

    @Override
    public MqttBrokerInfo getBrokerInfo() {
        return MqttBrokerInfo.builder()
                .host(mqttBrokerSettings.getHost())
                .mqttPort(mqttBrokerSettings.getMqttPort())
                .mqttsPort(mqttBrokerSettings.getMqttsPort())
                .wsPort(mqttBrokerSettings.getWsPort())
                .wssPort(mqttBrokerSettings.getWssPort())
                .wsPath(mqttBrokerSettings.getWsPath())
                .build();
    }

    protected void onPublish(MqttMessageEvent event) {
        eachListener(listener -> listener.onPublish(event));
    }

    protected void onConnect(MqttClientConnectEvent event) {
        eachListener(listener -> listener.onClientConnect(event));
    }

    protected void onDisconnect(MqttClientDisconnectEvent event) {
        eachListener(listener -> listener.onClientDisconnect(event));
    }

    protected void onBroadcast(MqttMessageEvent event) {
        eachListener(listener -> listener.onBroadcast(event));
    }

    private void eachListener(Consumer<MqttEventListener> consumer) {
        listeners.forEach(listener -> {
            try {
                executor.execute(() -> {
                    try {
                        consumer.accept(listener);
                    } catch (Exception e) {
                        log.warn("failed to handle the mqtt event.", e);
                    }
                });
            } catch (Exception e) {
                log.error("executor error.", e);
            }
        });
    }

    public void addListener(MqttEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MqttEventListener listener) {
        listeners.remove(listener);
    }

}
