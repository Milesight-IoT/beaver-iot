package com.milesight.beaveriot.rule.components.email;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfig {

    private EmailProvider provider;

    private SmtpConfig smtpConfig;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SmtpConfig {

        private String host;

        private Integer port;

        private String username;

        private String password;

        private SmtpEncryption encryption;

    }

}
