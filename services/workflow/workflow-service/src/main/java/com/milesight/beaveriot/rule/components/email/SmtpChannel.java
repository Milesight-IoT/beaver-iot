package com.milesight.beaveriot.rule.components.email;

import lombok.extern.slf4j.*;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;


@Slf4j
public class SmtpChannel implements EmailChannel {

    private final Mailer mailer;

    private final EmailConfig.SmtpConfig smtpConfig;

    public SmtpChannel(EmailConfig.SmtpConfig smtpConfig, ExecutorService executorService) {
        log.info("Init smtp channel");
        this.smtpConfig = smtpConfig;
        mailer = MailerBuilder
                // Vanilla SMTP with an insecure STARTTLS upgrade (if supported).
                .withTransportStrategy(smtpConfig.getEncryption() == null
                        ? TransportStrategy.SMTP
                        : smtpConfig.getEncryption().getStrategy())
                .withSMTPServer(
                        smtpConfig.getHost(),
                        smtpConfig.getPort(),
                        smtpConfig.getUsername(),
                        smtpConfig.getPassword()
                )
                .withSessionTimeout(15 * 1000)
                .clearEmailValidator()
                .withExecutorService(executorService)
                .buildMailer();
        log.info("SMTP channel init successfully");
    }

    public void send(String fromName, String fromAddress, List<String> to, String subject, String content) {
        log.info("Send email to: {}, subject: {}", to, subject);
        mailer.sendMail(EmailBuilder.startingBlank()
                .from(fromName, fromAddress == null ? smtpConfig.getUsername() : fromAddress)
                .toMultiple(to)
                .withSubject(subject)
                .withHTMLText(content)
                .buildEmail())
                .join();
        log.debug("Send email successfully");
    }

}
