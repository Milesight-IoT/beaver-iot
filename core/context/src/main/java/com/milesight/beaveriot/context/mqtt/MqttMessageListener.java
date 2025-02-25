package com.milesight.beaveriot.context.mqtt;

import java.util.function.Consumer;

@FunctionalInterface
public interface MqttMessageListener extends Consumer<MqttMessage> {

}
