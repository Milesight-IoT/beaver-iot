package com.milesight.beaveriot.rule.components.httpin.model;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import org.springframework.web.util.UriTemplate;

import java.util.function.Consumer;

/**
 * ListenConfig class.
 *
 * @author simon
 * @date 2025/4/17
 */
@Data
public class ListenConfig {
    private String method;

    private UriTemplate urlTemplate;

    private Long credentialsId;

    private Consumer<HttpInRequestContent> cb;
}
