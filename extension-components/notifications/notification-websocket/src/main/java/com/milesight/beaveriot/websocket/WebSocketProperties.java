package com.milesight.beaveriot.websocket;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author loong
 * @date 2024/10/18 18:04
 */
@Data
@ConfigurationProperties(prefix = "websocket")
public class WebSocketProperties {

    private Integer port;
    private String contextPath;

}
