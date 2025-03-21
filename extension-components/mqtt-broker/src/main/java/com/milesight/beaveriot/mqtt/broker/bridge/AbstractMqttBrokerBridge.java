package com.milesight.beaveriot.mqtt.broker.bridge;

import com.milesight.beaveriot.context.mqtt.MqttBrokerInfo;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAuthProvider;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.MqttEventListener;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.MqttMessageEvent;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

@Slf4j
public abstract class AbstractMqttBrokerBridge implements MqttBrokerBridge {

    private final List<MqttEventListener> listeners = new CopyOnWriteArrayList<>();

    @Getter
    private final MqttAuthProvider mqttAuthProvider;

    @Autowired
    protected MqttBrokerSettings mqttBrokerSettings;

    @Autowired
    @Qualifier("mqttBrokerBridgeListenerForkJoinPool")
    private ForkJoinPool listenerForkJoinPool;

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
                .build();
    }

    protected void onPublish(MqttMessageEvent event) {
        parallelEachListener(listener -> listener.onPublish(event));
    }

    protected void onBroadcast(MqttMessageEvent event) {
        parallelEachListener(listener -> listener.onBroadcast(event));
    }

    private void parallelEachListener(Consumer<MqttEventListener> consumer) {
        listenerForkJoinPool.submit(() ->
                listeners.parallelStream().forEach(listener -> {
                    try {
                        consumer.accept(listener);
                    } catch (Exception e) {
                        log.warn("A listener failed to handle the message.", e);
                    }
                }));
    }

    public void addListener(MqttEventListener listener) {
        listeners.add(listener);
    }

    public void removeListener(MqttEventListener listener) {
        listeners.remove(listener);
    }

}
