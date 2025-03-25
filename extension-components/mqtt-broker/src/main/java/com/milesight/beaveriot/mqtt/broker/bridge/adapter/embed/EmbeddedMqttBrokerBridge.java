package com.milesight.beaveriot.mqtt.broker.bridge.adapter.embed;

import com.milesight.beaveriot.mqtt.broker.bridge.AbstractMqttBrokerBridge;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAcl;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAction;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAuthProvider;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttClientConnectEvent;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttClientDisconnectEvent;
import com.milesight.beaveriot.mqtt.broker.bridge.listener.event.MqttMessageEvent;
import io.moquette.broker.Server;
import io.moquette.broker.config.MemoryConfig;
import io.moquette.broker.security.IAuthenticator;
import io.moquette.broker.security.IAuthorizatorPolicy;
import io.moquette.broker.subscriptions.Topic;
import io.moquette.interception.AbstractInterceptHandler;
import io.moquette.interception.messages.InterceptConnectMessage;
import io.moquette.interception.messages.InterceptConnectionLostMessage;
import io.moquette.interception.messages.InterceptPublishMessage;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.*;
import lombok.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

@Slf4j
public class EmbeddedMqttBrokerBridge extends AbstractMqttBrokerBridge {

    private final Server mqttBroker;

    private final MemoryConfig config;

    private final AbstractInterceptHandler interceptHandler = new DefaultInterceptHandler();

    private final IAuthenticator authenticator = new DefaultAuthenticator();

    private final IAuthorizatorPolicy authorizatorPolicy = new DefaultAuthorizatorPolicy();

    protected EmbeddedMqttBrokerBridge(MqttAuthProvider mqttAuthProvider, Properties properties) {
        super(mqttAuthProvider);
        mqttBroker = new Server();
        config = new MemoryConfig(properties);
    }

    @Override
    public void open() throws IOException {
        mqttBroker.startServer(config, List.of(interceptHandler), null, authenticator, authorizatorPolicy);
    }

    @Override
    public void close() {
        mqttBroker.stopServer();
    }

    @Override
    public void publish(String topic, byte[] payload, MqttQoS qos, boolean retained) {
        val message = MqttMessageBuilders.publish()
                .topicName(topic)
                .retained(retained)
                .qos(qos)
                .payload(Unpooled.copiedBuffer(payload))
                .build();
        mqttBroker.internalPublish(message, "ADMIN");

        // internal publish won't trigger publish listener, so we need to trigger it manually
        onPublish(new MqttMessageEvent(topic, payload));
        onBroadcast(new MqttMessageEvent(topic, payload));
    }

    @Override
    public void addUser(String username, String password) {
        // do nothing
    }

    @Override
    public void deleteUser(String username) {
        // do nothing
    }

    @Override
    public void addAcl(MqttAcl acl) {
        // do nothing
    }

    @Override
    public void deleteAcl(String username) {
        // do nothing
    }

    private class DefaultInterceptHandler extends AbstractInterceptHandler {

        @Override
        public void onPublish(InterceptPublishMessage msg) {
            val topic = msg.getTopicName();
            val payload = msg.getPayload().toString(StandardCharsets.UTF_8);
            log.debug("Received on topic: '" + topic + "', content: '" + payload + "'");
            EmbeddedMqttBrokerBridge.this.onPublish(new MqttMessageEvent(topic, payload.getBytes()));
            EmbeddedMqttBrokerBridge.this.onBroadcast(new MqttMessageEvent(topic, payload.getBytes()));
            super.onPublish(msg);
        }

        @Override
        public void onConnect(InterceptConnectMessage msg) {
            EmbeddedMqttBrokerBridge.this.onConnect(new MqttClientConnectEvent(msg.getClientID(), msg.getUsername(), System.currentTimeMillis()));
        }

        @Override
        public void onConnectionLost(InterceptConnectionLostMessage msg) {
            EmbeddedMqttBrokerBridge.this.onDisconnect(new MqttClientDisconnectEvent(msg.getClientID(), msg.getUsername(), System.currentTimeMillis()));
        }

        @Override
        public String getID() {
            return "DEFAULT_PUBLISH_LISTENER";
        }

        @Override
        public void onSessionLoopError(Throwable throwable) {
            log.warn("session loop error.", throwable);
        }
    }

    private class DefaultAuthenticator implements IAuthenticator {

        @Override
        public boolean checkValid(String clientId, String username, byte[] password) {
            if (password == null) {
                return true;
            }
            return EmbeddedMqttBrokerBridge.this.getMqttAuthProvider().canLogin(clientId, username, new String(password));
        }

    }

    private class DefaultAuthorizatorPolicy implements IAuthorizatorPolicy {

        @Override
        public boolean canWrite(Topic topic, String username, String client) {
            return EmbeddedMqttBrokerBridge.this.getMqttAuthProvider().canDo(MqttAction.PUBLISH, topic.toString(), client, username);
        }

        @Override
        public boolean canRead(Topic topic, String username, String client) {
            return EmbeddedMqttBrokerBridge.this.getMqttAuthProvider().canDo(MqttAction.SUBSCRIBE, topic.toString(), client, username);
        }

    }

}
