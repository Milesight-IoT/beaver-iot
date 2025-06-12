package com.milesight.beaveriot.mqtt.broker.bridge;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Component
@ConfigurationProperties(prefix = "mqtt.broker")
public class MqttBrokerSettings {

    private String host;

    @NotNull
    private Integer mqttPort;

    private Integer mqttsPort;

    @NotEmpty
    private String wsPath;

    private Integer wsPort;

    private Integer wssPort;

    private Moquette moquette;

    @Valid
    private Emqx emqx;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Moquette {
        private Integer port;
        private String host;
        private Boolean allowAnonymous;
        private Boolean persistenceEnabled;
        private Integer websocketPort;
        private String websocketPath;
        private Integer nettyMessageSize;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Emqx {
        @NotEmpty
        private String internalMqttEndpoint;
        @NotEmpty
        private String internalMqttUsername;
        @NotEmpty
        private String internalMqttPassword;
        @NotEmpty
        private String restApiEndpoint;
        @NotEmpty
        private String restApiUsername;
        @NotEmpty
        private String restApiPassword;
    }

}
