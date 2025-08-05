package com.milesight.beaveriot.mqtt.service;

import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.mqtt.enums.MqttQos;
import com.milesight.beaveriot.context.mqtt.enums.MqttTopicChannel;
import com.milesight.beaveriot.context.mqtt.listener.MqttConnectEventListener;
import com.milesight.beaveriot.context.mqtt.listener.MqttDisconnectEventListener;
import com.milesight.beaveriot.context.mqtt.listener.MqttMessageListener;
import com.milesight.beaveriot.context.mqtt.listener.MqttPubSubServiceListener;
import com.milesight.beaveriot.context.mqtt.model.MqttBrokerInfo;
import com.milesight.beaveriot.context.mqtt.model.MqttConnectEvent;
import com.milesight.beaveriot.context.mqtt.model.MqttDisconnectEvent;
import com.milesight.beaveriot.context.mqtt.model.MqttMessage;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.mqtt.api.MqttAdminPubSubServiceProvider;
import com.milesight.beaveriot.mqtt.broker.bridge.MqttBrokerBridge;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.MqttEventListener;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttClientConnectEvent;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttClientDisconnectEvent;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttMessageEvent;
import io.moquette.broker.subscriptions.Token;
import io.moquette.broker.subscriptions.Topic;
import io.netty.handler.codec.mqtt.MqttQoS;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

@Order(10000)
@Slf4j
@Service
public class MqttPubSubService implements MqttAdminPubSubServiceProvider, CommandLineRunner {

    private static final String SINGLE_LEVEL_WILDCARD = "+";

    /**
     * Topic Subscribers
     * <p>
     * username -> topic -> callback -> isSharedSubscription
     */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<Topic, ConcurrentHashMap<MqttMessageListener, Boolean>>> subscribers = new ConcurrentHashMap<>();

    /**
     * Subscriber Index
     * <p>
     * callback -> callbacks sets
     */
    private static final ConcurrentHashMap<MqttMessageListener, ConcurrentHashMap<Topic, Boolean>> subscriberIndex = new ConcurrentHashMap<>();

    /**
     * MQTT Client Connect Event Listeners
     */
    private static final CopyOnWriteArraySet<MqttConnectEventListener> connectEventListeners = new CopyOnWriteArraySet<>();

    /**
     * MQTT Client Disconnect Event Listeners
     */
    private static final CopyOnWriteArraySet<MqttDisconnectEventListener> disconnectEventListeners = new CopyOnWriteArraySet<>();

    private static final ConcurrentLinkedQueue<UnreadyInboundMessage> blockedInboundMessages = new ConcurrentLinkedQueue<>();

    private static final ConcurrentLinkedQueue<UnreadyOutboundMessage> blockedOutboundMessages = new ConcurrentLinkedQueue<>();

    private final AtomicBoolean isReady = new AtomicBoolean(false);

    @Autowired
    private MqttBrokerBridge mqttBrokerBridge;

    @Autowired
    private CredentialsServiceProvider credentialsServiceProvider;

    @Qualifier("mqtt-subscriber")
    @Autowired
    private TaskExecutor executor;

    private void fireEvent(MqttMessageEvent event, boolean broadcast) {
        boolean isNotReady = runWithLockIfServiceNotReady(() ->
                blockedInboundMessages.add(new UnreadyInboundMessage(event, broadcast)));
        if (isNotReady) {
            return;
        }

        val topic = new Topic(event.getTopic());
        val topicTokens = topic.getTokens().stream().map(Token::toString).toList();
        if (topicTokens.size() < 2) {
            return;
        }

        val topicPrefix = topicTokens.get(0);
        val topicChannel = MqttTopicChannel.getByTopicPrefix(topicPrefix);
        if (topicChannel == null) {
            return;
        }

        val publisherUsername = topicTokens.get(1);
        val topicSubPath = String.join("/", topicTokens.subList(2, topicTokens.size()));
        val tenantId = updateTenantContextByUsername(publisherUsername);

        val mqttMessage = new MqttMessage(event.getTopic(), topicSubPath,
                topicChannel, publisherUsername, tenantId, topicTokens, event.getPayload());

        subscribers.forEach((subscriberUsername, topicSubscribers) -> {
            if (!publisherUsername.equals(subscriberUsername) && !SINGLE_LEVEL_WILDCARD.equals(subscriberUsername)) {
                return;
            }
            topicSubscribers.forEach((subscriptionTopic, callbacks) ->
                    callbacks.forEach((listener, sharedSubscription) -> {
                        if (Objects.equals(!broadcast, sharedSubscription)
                                && topic.match(subscriptionTopic)) {
                            try {
                                executor.execute(() -> {
                                    try {
                                        listener.accept(mqttMessage);
                                    } catch (Exception e) {
                                        log.warn("failed to handle the message. topic: '{}'.", subscriptionTopic, e);
                                    } finally {
                                        TenantContext.clear();
                                    }
                                });
                            } catch (Exception e) {
                                log.error("executor error.", e);
                            }
                        }
                    }));
        });
    }

    private boolean runWithLockIfServiceNotReady(Runnable job) {
        if (!isReady.get()) {
            synchronized (isReady) {
                if (!isReady.get()) {
                    job.run();
                    return true;
                }
            }
        }
        return false;
    }

    private static String updateTenantContextByUsername(String publisherUsername) {
        val usernameTokens = publisherUsername.split("@");
        if (usernameTokens.length != 2) {
            return null;
        }
        val tenantId = usernameTokens[1];
        if (tenantId.isEmpty()) {
            return null;
        }
        TenantContext.setTenantId(tenantId);
        return tenantId;
    }

    @PostConstruct
    protected void init() {
        mqttBrokerBridge.addListener(new MqttEventListener() {
            @Override
            public void onPublish(MqttMessageEvent event) {
                fireEvent(event, false);
            }

            @Override
            public void onBroadcast(MqttMessageEvent event) {
                fireEvent(event, true);
            }

            @Override
            public void onClientConnect(MqttClientConnectEvent event) {
                val tenantId = updateTenantContextByUsername(event.getUsername());
                if (tenantId == null) {
                    return;
                }
                val e = new MqttConnectEvent(tenantId, event.getClientId(), event.getUsername(), event.getTs());
                connectEventListeners.forEach(listener -> listener.accept(e));
            }

            @Override
            public void onClientDisconnect(MqttClientDisconnectEvent event) {
                val tenantId = updateTenantContextByUsername(event.getUsername());
                if (tenantId == null) {
                    return;
                }
                val e = new MqttDisconnectEvent(tenantId, event.getClientId(), event.getUsername(), event.getTs());
                disconnectEventListeners.forEach(listener -> listener.accept(e));
            }
        });

    }

    @Override
    public void publish(MqttTopicChannel mqttTopicChannel, String username, String topicSubPath, byte[] payload, MqttQos qos, boolean retained) {
        Assert.notNull(mqttTopicChannel, "mqttTopicChannel cannot be null");
        if (MqttTopicChannel.INTERNAL.equals(mqttTopicChannel)) {
            throw new IllegalArgumentException("cannot publish to internal topic");
        }
        val topicPrefix = mqttTopicChannel.getTopicPrefix();
        publish(topicPrefix, username, topicSubPath, payload, qos, retained);
    }

    @Override
    public void publish(String topicPrefix, String username, String topicSubPath, byte[] payload, MqttQos qos, boolean retained) {
        val topicName = getFullTopicName(topicPrefix, username, topicSubPath);
        val mqttQoS = MqttQoS.valueOf(qos.getValue());
        log.info("publish to topic: '{}'", topicName);
        boolean isNotReady = runWithLockIfServiceNotReady(() ->
                blockedOutboundMessages.add(new UnreadyOutboundMessage(topicName, payload, mqttQoS, retained)));
        if (!isNotReady) {
            mqttBrokerBridge.publish(topicName, payload, mqttQoS, retained);
        }
    }

    @Override
    public void subscribe(MqttTopicChannel mqttTopicChannel, String username, String topicSubPath, MqttMessageListener listener) {
        subscribe(mqttTopicChannel, username, topicSubPath, listener, true);
    }

    @Override
    public void subscribe(MqttTopicChannel mqttTopicChannel, String username, String topicSubPath, MqttMessageListener listener, boolean shared) {
        Assert.notNull(mqttTopicChannel, "mqttTopicChannel cannot be null");
        val topicPrefix = mqttTopicChannel.getTopicPrefix();
        subscribe(topicPrefix, username, topicSubPath, listener, shared);
    }

    @Override
    public void subscribe(String topicPrefix, String username, String topicSubPath, MqttMessageListener listener) {
        subscribe(topicPrefix, username, topicSubPath, listener, true);
    }

    @Override
    public void subscribe(String topicPrefix, String username, String topicSubPath, MqttMessageListener listener, boolean shared) {
        Assert.notNull(listener, "listener cannot be null");
        val topicName = getFullTopicName(topicPrefix, username, topicSubPath);
        val topic = new Topic(topicName);
        log.info("subscribe topic: '{}'", topic);
        val subscribersForUsername = subscribers.computeIfAbsent(username, k -> new ConcurrentHashMap<>());
        val subscribersForTopic = subscribersForUsername.computeIfAbsent(topic, k -> new ConcurrentHashMap<>());
        synchronized (subscriberIndex) {
            subscribersForTopic.put(listener, shared);
            subscriberIndex.computeIfAbsent(listener, k -> new ConcurrentHashMap<>()).put(topic, shared);
        }
    }

    @Override
    public void publish(String username, String topicSubPath, byte[] payload, MqttQos qos, boolean retained) {
        publish(MqttTopicChannel.DEFAULT, username, topicSubPath, payload, qos, retained);
    }

    @Override
    public void publish(String topicSubPath, byte[] payload, MqttQos qos, boolean retained) {
        val credentials = credentialsServiceProvider.getOrCreateCredentials(CredentialsType.MQTT);
        val username = credentials.getAccessKey();
        publish(username, topicSubPath, payload, qos, retained);
    }

    @Override
    public void subscribe(String username, String topicSubPath, MqttMessageListener onMessage, boolean shared) {
        subscribe(MqttTopicChannel.DEFAULT, username, topicSubPath, onMessage, shared);
    }

    @Override
    public void subscribe(String topicSubPath, MqttMessageListener onMessage, boolean shared) {
        subscribe(SINGLE_LEVEL_WILDCARD, topicSubPath, onMessage, shared);
    }

    @Override
    public void subscribe(String username, String topicSubPath, MqttMessageListener onMessage) {
        subscribe(MqttTopicChannel.DEFAULT, username, topicSubPath, onMessage);
    }

    @Override
    public void subscribe(String topicSubPath, MqttMessageListener onMessage) {
        subscribe(SINGLE_LEVEL_WILDCARD, topicSubPath, onMessage);
    }

    @Override
    public void unsubscribe(MqttPubSubServiceListener listener) {
        Assert.notNull(listener, "listener cannot be null");
        if (listener instanceof MqttMessageListener mqttMessageListener) {
            removeMqttMessageListener(mqttMessageListener);
        } else if (listener instanceof MqttConnectEventListener mqttConnectEventListener) {
            connectEventListeners.remove(mqttConnectEventListener);
        } else if (listener instanceof MqttDisconnectEventListener mqttDisconnectEventListener) {
            disconnectEventListeners.remove(mqttDisconnectEventListener);
        }
    }

    private void removeMqttMessageListener(MqttMessageListener listener) {
        synchronized (subscriberIndex) {
            val topics = subscriberIndex.get(listener);
            if (topics == null) {
                return;
            }
            topics.forEach((topic, shared) -> {
                val username = getUsernameFromTopic(topic);
                val subscribersForUsername = subscribers.get(username);
                if (subscribersForUsername == null) {
                    return;
                }
                val subscribersForTopic = subscribersForUsername.get(topic);
                if (subscribersForTopic == null) {
                    return;
                }
                log.info("unsubscribe from topic: '{}', listener: {}", topic, listener);
                subscribersForTopic.remove(listener);
                if (subscribersForTopic.isEmpty()) {
                    subscribersForUsername.remove(topic);
                }
                if (subscribersForUsername.isEmpty()) {
                    subscribers.remove(username);
                }
            });
            subscriberIndex.remove(listener);
        }
    }

    @Override
    public void unsubscribe(String username, String topicSubPath) {
        unsubscribe(MqttTopicChannel.DEFAULT, username, topicSubPath);
    }

    @Override
    public void unsubscribe(String topicSubPath) {
        unsubscribe(SINGLE_LEVEL_WILDCARD, topicSubPath);
    }

    @Override
    public void onConnect(MqttConnectEventListener listener) {
        connectEventListeners.add(listener);
    }

    @Override
    public void onDisconnect(MqttDisconnectEventListener listener) {
        disconnectEventListeners.add(listener);
    }

    @Override
    public void unsubscribe(MqttTopicChannel mqttTopicChannel, String username, String topicSubPath) {
        Assert.notNull(mqttTopicChannel, "mqttTopicChannel cannot be null");
        val topicPrefix = mqttTopicChannel.getTopicPrefix();
        unsubscribe(topicPrefix, username, topicSubPath);
    }

    @Override
    public void unsubscribe(String topicPrefix, String username, String topicSubPath) {
        val topic = new Topic(getFullTopicName(topicPrefix, username, topicSubPath));
        log.info("unsubscribe from topic: '{}'", topic);
        val subscribersForUsername = subscribers.get(username);
        if (subscribersForUsername == null) {
            return;
        }
        var subscribersForTopic = subscribersForUsername.get(topic);
        if (subscribersForTopic == null) {
            return;
        }
        synchronized (subscriberIndex) {
            subscribersForTopic.forEach((listener, shared) -> {
                val topics = subscriberIndex.get(listener);
                if (topics == null) {
                    return;
                }
                topics.remove(topic);
                if (topics.isEmpty()) {
                    subscriberIndex.remove(listener);
                }
            });
            subscribersForUsername.remove(topic);
            if (subscribersForUsername.isEmpty()) {
                subscribers.remove(username);
            }
        }
    }

    @Override
    public String getFullTopicName(String username, String topicSubPath) {
        return getFullTopicName(MqttTopicChannel.DEFAULT.getTopicPrefix(), username, topicSubPath);
    }

    @Override
    public String getFullTopicName(String topicSubPath) {
        return getFullTopicName(SINGLE_LEVEL_WILDCARD, topicSubPath);
    }

    public String getFullTopicName(String topicPrefix, String username, String topicSubPath) {
        Assert.notNull(topicPrefix, "topicPrefix cannot be null");
        Assert.notNull(username, "username cannot be null");
        Assert.notNull(topicSubPath, "topicSubPath cannot be null");
        if (topicPrefix.startsWith("/")) {
            topicPrefix = topicPrefix.substring(1);
        }
        return String.join("/", topicPrefix, username, topicSubPath);
    }

    public String getUsernameFromTopic(Topic topic) {
        return topic.getTokens().get(1).toString();
    }

    public MqttBrokerInfo getMqttBrokerInfo() {
        return mqttBrokerBridge.getBrokerInfo();
    }

    @Override
    public void run(String... args) {
        // Start sending messages only when the server is ready.
        synchronized (isReady) {
            log.info("Ready to dispatch MQTT messages.");
            isReady.set(true);
            blockedInboundMessages.forEach(m -> fireEvent(m.mqttMessage, m.broadcast));
            blockedOutboundMessages.forEach(m -> mqttBrokerBridge.publish(m.topic, m.payload, m.qos, m.retained));
        }
    }

    @SuppressWarnings("java:S6218")
    private record UnreadyInboundMessage(MqttMessageEvent mqttMessage, boolean broadcast) {
    }

    @SuppressWarnings("java:S6218")
    private record UnreadyOutboundMessage(String topic, byte[] payload, MqttQoS qos, boolean retained) {
    }

}
