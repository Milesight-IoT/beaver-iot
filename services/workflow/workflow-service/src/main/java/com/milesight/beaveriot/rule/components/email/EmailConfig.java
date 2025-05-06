package com.milesight.beaveriot.rule.components.email;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailConfig {

    private EmailProvider provider;

    private SmtpConfig smtpConfig;

    private Boolean useSystemSettings;

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

        public enum Type {
            DEFAULT,
            CREDENTIALS,
            ;
        }

    }

}
