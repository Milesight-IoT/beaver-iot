package com.milesight.beaveriot.context.i18n.configuration;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.autoconfigure.context.MessageSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * author: Luxb
 * create: 2025/8/1 10:55
 **/
@EqualsAndHashCode(callSuper = true)
@Data
@Component
@ConfigurationProperties(prefix = "spring.messages")
public class MessageSourceConfig extends MessageSourceProperties {
}