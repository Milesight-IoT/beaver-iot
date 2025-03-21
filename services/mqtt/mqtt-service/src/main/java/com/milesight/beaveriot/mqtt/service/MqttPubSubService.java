package com.milesight.beaveriot.mqtt.service;

import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.mqtt.MqttBrokerInfo;
import com.milesight.beaveriot.context.mqtt.MqttMessage;
import com.milesight.beaveriot.context.mqtt.MqttMessageListener;
import com.milesight.beaveriot.context.mqtt.MqttQos;
import com.milesight.beaveriot.context.mqtt.MqttTopicChannel;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.mqtt.api.MqttAdminPubSubServiceProvider;
import com.milesight.beaveriot.mqtt.broker.bridge.MqttBrokerBridge;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.MqttEventListener;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.MqttMessageEvent;
import io.moquette.broker.subscriptions.Token;
import io.moquette.broker.subscriptions.Topic;
import io.netty.handler.codec.mqtt.MqttQoS;
import jakarta.annotation.PostConstruct;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttPubSubService implements MqttAdminPubSubServiceProvider {

    private static final String SINGLE_LEVEL_WILDCARD = "+";

    /**
     * Topic Subscribers <br>
     * username -> topic -> callback -> isSharedSubscription
     */
    private static final ConcurrentHashMap<String, ConcurrentHashMap<Topic, ConcurrentHashMap<MqttMessageListener, Boolean>>> subscribers = new ConcurrentHashMap<>();

    /**
     * Subscriber Index <br>
     * callback -> callbacks sets
     */
    private static final ConcurrentHashMap<MqttMessageListener, ConcurrentHashMap<Topic, Boolean>> subscriberIndex = new ConcurrentHashMap<>();

    private final MqttBrokerBridge mqttBrokerBridge;

    private final CredentialsServiceProvider credentialsServiceProvider;

    private static void fireEvent(MqttMessageEvent event, boolean broadcast) {
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
        val topicSubPath = String.join("", topicTokens.subList(2, topicTokens.size()));
        val usernameTokens = publisherUsername.split("@");
        String tenantId = null;
        if (usernameTokens.length == 2 && !usernameTokens[1].isEmpty()) {
            tenantId = usernameTokens[1];
            TenantContext.setTenantId(tenantId);
        }

        val mqttMessage = new MqttMessage(event.getTopic(), topicSubPath,
                topicChannel, publisherUsername, tenantId, topicTokens, event.getPayload());

        subscribers.forEach((subscriberUsername, topicSubscribers) -> {
            if (!publisherUsername.equals(subscriberUsername) && !SINGLE_LEVEL_WILDCARD.equals(subscriberUsername)) {
                return;
            }
            topicSubscribers.forEach((subscriptionTopic, callbacks) ->
                    callbacks.forEach((listener, sharedSubscription) -> {
                        if (Objects.equals(!broadcast, sharedSubscription)) {
                            listener.accept(mqttMessage);
                        }
                    }));
        });
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
        log.info("publish to topic: '{}'", topicName);
        mqttBrokerBridge.publish(topicName, payload, MqttQoS.valueOf(qos.getValue()), retained);
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
        val credentials = credentialsServiceProvider.getOrCreateDefaultCredentials(CredentialsType.MQTT);
        val username = "%s@%s".formatted(credentials.getAccessKey(), TenantContext.getTenantId());
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
    public void unsubscribe(MqttMessageListener listener) {
        Assert.notNull(listener, "listener cannot be null");
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

}
