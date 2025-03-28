package com.milesight.beaveriot.mqtt.broker.bridge.adapter.emqx;

import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.mqtt.broker.bridge.AbstractMqttBrokerBridge;
import com.milesight.beaveriot.mqtt.broker.bridge.adapter.emqx.model.EmqxAcl;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAcl;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAuthProvider;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttClientConnectEvent;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttClientDisconnectEvent;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttMessageEvent;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.*;
import lombok.extern.slf4j.*;
import org.eclipse.paho.mqttv5.client.IMqttAsyncClient;
import org.eclipse.paho.mqttv5.client.IMqttToken;
import org.eclipse.paho.mqttv5.client.MqttActionListener;
import org.eclipse.paho.mqttv5.client.MqttAsyncClient;
import org.eclipse.paho.mqttv5.client.MqttCallback;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.MqttDisconnectResponse;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttMessage;
import org.eclipse.paho.mqttv5.common.MqttSubscription;
import org.eclipse.paho.mqttv5.common.packet.MqttProperties;
import org.springframework.util.Assert;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
public class EmqxMqttBrokerBridge extends AbstractMqttBrokerBridge {

    public static final String DEFAULT_SHARED_SUBSCRIPTION_TOPIC_FILTER = "$share/beaver-iot-server/#";

    public static final String CLIENT_SYS_EVENT_TOPIC_FILTER = "$SYS/brokers/+/clients/+/#";

    public static final String DEFAULT_TOPIC_FILTER = "#";

    private final EmqxRestApi emqxRestApi;

    /**
     * Solve <a href="https://github.com/eclipse-paho/paho.mqtt.java/issues/827">this</a>
     */
    private IMqttAsyncClient sharedSubscriptionMqttClient;

    private IMqttAsyncClient broadcastMqttClient;

    @SneakyThrows
    protected EmqxMqttBrokerBridge(MqttAuthProvider mqttAuthProvider, EmqxRestApi emqxRestApi) {
        super(mqttAuthProvider);
        this.emqxRestApi = emqxRestApi;
    }

    @SneakyThrows
    @Override
    public synchronized void open() {
        if (sharedSubscriptionMqttClient == null) {
            sharedSubscriptionMqttClient = createMqttClient();
        }
        if (broadcastMqttClient == null) {
            broadcastMqttClient = createMqttClient();
        }

        emqxRestApi.ensureAuthenticator();
        emqxRestApi.ensureMqttAdminUser(mqttBrokerSettings.getEmqx().getInternalMqttUsername(), mqttBrokerSettings.getEmqx().getInternalMqttPassword());
        emqxRestApi.ensureAuthorizationSource();

        sharedSubscriptionMqttClient.setCallback(new MqttSubscriptionCallback(sharedSubscriptionMqttClient, List.of(DEFAULT_SHARED_SUBSCRIPTION_TOPIC_FILTER)) {

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                log.debug("received shared subscription message from emqx mqtt broker, topic: '{}', message: '{}'", topic, message);
                EmqxMqttBrokerBridge.this.onPublish(new MqttMessageEvent(topic, message.getPayload()));
            }

        });

        broadcastMqttClient.setCallback(new MqttSubscriptionCallback(broadcastMqttClient, List.of(DEFAULT_TOPIC_FILTER, CLIENT_SYS_EVENT_TOPIC_FILTER)) {

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                log.debug("received broadcast message from emqx mqtt broker, topic: '{}', message: '{}'", topic, message);
                if (!topic.startsWith("$SYS/")) {
                    EmqxMqttBrokerBridge.this.onBroadcast(new MqttMessageEvent(topic, message.getPayload()));
                } else {
                    val content = JsonUtils.fromJSON(new String(message.getPayload()));
                    if (content == null) {
                        return;
                    }
                    val clientId = content.get("clientid").textValue();
                    val username = content.get("username").textValue();
                    val ts = content.get("ts").asLong();
                    // todo deduplication
                    if (topic.endsWith("/connected")) {
                        EmqxMqttBrokerBridge.this.onConnect(new MqttClientConnectEvent(clientId, username, ts));
                    } else if (topic.endsWith("/disconnected")) {
                        EmqxMqttBrokerBridge.this.onDisconnect(new MqttClientDisconnectEvent(clientId, username, ts));
                    }
                }
            }

        });

        sharedSubscriptionMqttClient.connect(createMqttConnectionOptions(), null, new MqttConnectionCallback("sharedSubscriptionMqttClient")).waitForCompletion(10000);
        broadcastMqttClient.connect(createMqttConnectionOptions(), null, new MqttConnectionCallback("sharedSubscriptionMqttClient")).waitForCompletion(10000);
    }

    private MqttConnectionOptions createMqttConnectionOptions() {
        val options = new MqttConnectionOptions();
        options.setUserName(mqttBrokerSettings.getEmqx().getInternalMqttUsername());
        options.setPassword(mqttBrokerSettings.getEmqx().getInternalMqttPassword().getBytes(StandardCharsets.UTF_8));
        options.setAutomaticReconnect(true);
        options.setCleanStart(true);
        return options;
    }

    private MqttAsyncClient createMqttClient() throws MqttException {
        return new MqttAsyncClient(mqttBrokerSettings.getEmqx().getInternalMqttEndpoint(),
                "beaver-iot-server#" + SnowflakeUtil.nextId(), new MemoryPersistence());
    }

    @SneakyThrows
    @Override
    public synchronized void close() {
        sharedSubscriptionMqttClient.disconnect();
        sharedSubscriptionMqttClient.close();
    }

    @Override
    @SneakyThrows
    public void publish(String topic, byte[] payload, MqttQoS qos, boolean retained) {
        sharedSubscriptionMqttClient.publish(topic, payload, qos.value(), retained);
    }

    @Override
    public void addUser(String username, String password) {
        emqxRestApi.addUser(username, password, false);
    }

    @Override
    public void deleteUser(String username) {
        emqxRestApi.deleteUser(username);
    }

    @Override
    public void addAcl(MqttAcl acl) {
        Assert.notNull(acl, "acl must not be null");
        Assert.notNull(acl.getUsername(), "acl username must not be null");
        Assert.notEmpty(acl.getRules(), "acl rules must not be empty");

        emqxRestApi.addAclRule(EmqxAcl.builder()
                .username(acl.getUsername())
                .rules(acl.getRules().stream()
                        .map(rule -> EmqxAcl.Rule.builder()
                                .topic(rule.getTopic())
                                .action(EmqxAcl.Action.valueOf(rule.getAction().name()))
                                .permission(EmqxAcl.Permission.valueOf(rule.getPermission().name()))
                                .qos(rule.getQos())
                                .retain(rule.getRetain())
                                .build())
                        .toList())
                .build());
    }

    @Override
    public void deleteAcl(String username) {
        emqxRestApi.deleteAclRule(username);
    }

    private record MqttConnectionCallback(String clientName) implements MqttActionListener {

        @Override
        public void onSuccess(IMqttToken asyncActionToken) {
            // do nothing
        }

        @Override
        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
            log.error("{} failed to connect to emqx mqtt broker", clientName, exception);
        }
    }

    @RequiredArgsConstructor
    private abstract static class MqttSubscriptionCallback implements MqttCallback {

        protected final IMqttAsyncClient client;
        protected final List<String> topics;

        @Override
        public void disconnected(MqttDisconnectResponse disconnectResponse) {
            log.info("disconnect from emqx mqtt broker: {}", disconnectResponse.getReasonString());
        }

        @Override
        public void mqttErrorOccurred(MqttException exception) {
            log.warn("mqtt error occurred:", exception);
        }

        @Override
        public void deliveryComplete(IMqttToken token) {
            // do nothing
        }

        @Override
        public void authPacketArrived(int reasonCode, MqttProperties properties) {
            // do nothing
        }

        @SneakyThrows
        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            if (reconnect) {
                log.info("reconnect to emqx mqtt broker: '{}' ({})", serverURI, String.join(", ", topics));
            } else {
                log.info("connect to emqx mqtt broker: '{}' ({})", serverURI, String.join(", ", topics));
            }

            topics.forEach(topic -> {
                val subscriptionProperties = new MqttProperties();
                subscriptionProperties.setSubscriptionIdentifiers(List.of(0));
                try {
                    client.subscribe(new MqttSubscription(topic), null, null, null, subscriptionProperties).waitForCompletion(10000);
                    log.info("subscribed topic: '{}'", topic);
                } catch (Exception e) {
                    log.error("subscribe topic failed: {}", topic, e);
                }
            });
        }

    }

}
