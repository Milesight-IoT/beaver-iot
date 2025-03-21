package com.milesight.beaveriot.pubsub;


import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.pubsub.api.message.LocalUnicastMessage;
import com.milesight.beaveriot.pubsub.api.message.PubSubMessage;
import com.milesight.beaveriot.pubsub.api.message.RemoteBroadcastMessage;
import lombok.*;
import lombok.extern.slf4j.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.function.Consumer;

/**
 * A simple pub/sub implementation for broadcasting message between peers.
 * Notice that it's not designed for peer to peer communication.
 */
@Slf4j
@RequiredArgsConstructor
public abstract class MessagePubSub {

    private final MessageRouter messageRouter;

    public <T extends PubSubMessage> void subscribe(Class<T> clazz, Consumer<T> listener) {
        messageRouter.subscribe(clazz, listener);
    }

    /**
     * Publish message after transaction commit
     */
    public void publishAfterCommit(PubSubMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronization transactionSynchronization = new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublishMessage(message);
                }
            };
            TransactionSynchronizationManager.registerSynchronization(transactionSynchronization);
        } else {
            doPublishMessage(message);
        }
    }

    public void publish(PubSubMessage message) {
        doPublishMessage(message);
    }

    protected void doPublishMessage(PubSubMessage message) {
        if (message.getTenantId() == null) {
            TenantContext.tryGetTenantId().ifPresent(message::setTenantId);
        }

        if (message instanceof RemoteBroadcastMessage remoteBroadcastMessage) {
            var serializableMessage = new SerializableMessage(
                    message.getClass().getName(),
                    SnowflakeUtil.nextId(),
                    System.currentTimeMillis(),
                    JsonUtils.toJSON(remoteBroadcastMessage)
            );
            remoteBroadcast(JsonUtils.toJSON(serializableMessage));
        } else if (message instanceof LocalUnicastMessage localUnicastMessage) {
            localPublish(localUnicastMessage);
        } else {
            throw new IllegalArgumentException("message type is not supported");
        }
    }

    private void localPublish(LocalUnicastMessage message) {
        messageRouter.dispatch(message);
    }

    protected abstract void remoteBroadcast(String serializedMessage);

    protected void onRemoteBroadcastMessage(String serializedMessage) {
        log.debug("onRemoteBroadcastMessage: {}", serializedMessage);
        try {
            var rawMessage = JsonUtils.fromJSON(serializedMessage, SerializableMessage.class);
            if (rawMessage == null) {
                log.error("serializedMessage is null");
                return;
            }
            onRemoteBroadcastMessage(rawMessage);
        } catch (Exception e) {
            log.error("handle RemoteBroadcastMessage failed.", e);
        }
    }

    private void onRemoteBroadcastMessage(SerializableMessage rawMessage) {
        var className = rawMessage.className;
        if (className == null || className.isEmpty()) {
            log.error("className is null or empty");
            return;
        }
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            log.error("class not found: {}", className);
            return;
        }
        var message = (PubSubMessage) JsonUtils.fromJSON(rawMessage.json, clazz);
        messageRouter.dispatch(message);
    }

    private record SerializableMessage(String className, Long id, Long timestamp, String json) {
    }

}
