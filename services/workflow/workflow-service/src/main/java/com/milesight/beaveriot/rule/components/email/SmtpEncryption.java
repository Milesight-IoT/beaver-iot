package com.milesight.beaveriot.rule.components.email;

import lombok.*;
import org.simplejavamail.api.mailer.config.TransportStrategy;

/**
 *
 */
@Getter
@RequiredArgsConstructor
public enum SmtpEncryption {
    TLS(TransportStrategy.SMTPS),
    STARTTLS(TransportStrategy.SMTP_TLS),
    NONE(TransportStrategy.SMTP),
    ;

    private final TransportStrategy strategy;

}
