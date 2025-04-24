package com.milesight.beaveriot.mqtt.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.integration.model.Credentials;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.context.util.SecretUtils;
import com.milesight.beaveriot.credentials.api.model.CredentialsChangeEvent;
import com.milesight.beaveriot.mqtt.api.MqttAdminPubSubServiceProvider;
import com.milesight.beaveriot.mqtt.broker.bridge.MqttBrokerBridge;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAcl;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAction;
import com.milesight.beaveriot.mqtt.broker.bridge.auth.MqttAuthProvider;
import com.milesight.beaveriot.mqtt.model.WebMqttCredentials;
import com.milesight.beaveriot.pubsub.api.annotation.MessageListener;
import lombok.extern.slf4j.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class MqttAclService implements MqttAuthProvider {

    public static final String DEFAULT_TOPIC_PREFIX = "beaver-iot";

    public static final String CREDENTIALS_TYPE = CredentialsType.MQTT.name();

    @Lazy
    @Autowired
    private CredentialsServiceProvider credentialsServiceProvider;

    @Lazy
    @Autowired
    private MqttBrokerBridge mqttBrokerBridge;

    @MessageListener
    public void onCredentialsChange(CredentialsChangeEvent event) {
        val credentials = event.getCredentials();
        if (!CREDENTIALS_TYPE.equalsIgnoreCase(credentials.getCredentialsType())) {
            return;
        }

        val usernameTokens = credentials.getAccessKey().split("@");
        if (usernameTokens.length != 2 || usernameTokens[0].isEmpty() || usernameTokens[1].isEmpty()) {
            throw new IllegalArgumentException("Invalid MQTT username");
        }

        switch (event.getOperation()) {
            case ADD -> {
                val topicPattern = String.format("%s/%s/%s", DEFAULT_TOPIC_PREFIX, credentials.getAccessKey(), "#");
                mqttBrokerBridge.addUser(credentials.getAccessKey(), credentials.getAccessSecret());
                mqttBrokerBridge.addAcl(MqttAcl.builder()
                        .username(credentials.getAccessKey())
                        .rules(List.of(
                                MqttAcl.Rule.builder()
                                        .topic(topicPattern)
                                        .action(MqttAcl.Action.SUBSCRIBE)
                                        .permission(MqttAcl.Permission.ALLOW)
                                        .build(),
                                MqttAcl.Rule.builder()
                                        .topic(topicPattern)
                                        .action(MqttAcl.Action.PUBLISH)
                                        .permission(MqttAcl.Permission.ALLOW)
                                        .build(),
                                MqttAcl.Rule.builder()
                                        .topic("#")
                                        .action(MqttAcl.Action.SUBSCRIBE)
                                        .permission(MqttAcl.Permission.DENY)
                                        .build(),
                                MqttAcl.Rule.builder()
                                        .topic("#")
                                        .action(MqttAcl.Action.PUBLISH)
                                        .permission(MqttAcl.Permission.DENY)
                                        .build()
                        ))
                        .build());
            }
            case DELETE -> {
                mqttBrokerBridge.deleteUser(credentials.getAccessKey());
                mqttBrokerBridge.deleteAcl(credentials.getAccessKey());
            }
            default -> {
                // do nothing
            }
        }
    }

    @Override
    public boolean canDo(MqttAction action, String topic, String clientId, String username) {
        val tenantId = getTenantIdFromUsername(username);
        if (tenantId == null) {
            return true;
        }
        TenantContext.setTenantId(tenantId);
        val topicTokens = topic.split("/");
        if (topicTokens.length < 2 || !Objects.equals(topicTokens[1], username)) {
            return false;
        }
        val topicPrefix = topicTokens[0];
        return DEFAULT_TOPIC_PREFIX.equals(topicPrefix);
    }

    @Override
    public boolean canLogin(String clientId, String username, String password) {
        val tenantId = getTenantIdFromUsername(username);
        if (tenantId == null) {
            return false;
        }
        TenantContext.setTenantId(tenantId);
        return credentialsServiceProvider.getCredentials(CREDENTIALS_TYPE, username)
                .map(Credentials::getAccessSecret)
                .map(password::equals)
                .orElse(false);
    }

    private String getTenantIdFromUsername(String username) {
        if (username.indexOf('@') < 0) {
            return null;
        }
        return username.split("@")[1];
    }

    public WebMqttCredentials getOrInitWebMqttCredentials() {
        val tenantId = TenantContext.getTenantId();
        val username = MqttAdminPubSubServiceProvider.getWebUsername(tenantId);
        var credentials = credentialsServiceProvider.getOrCreateCredentials(CREDENTIALS_TYPE, username, SecretUtils.randomSecret(32));
        return WebMqttCredentials.builder()
                .clientId(String.format("%s#%s", username, SnowflakeUtil.nextId()))
                .username(username)
                .password(credentials.getAccessSecret())
                .build();
    }

}
