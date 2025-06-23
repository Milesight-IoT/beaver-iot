package com.milesight.beaveriot.rule.components.email;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.CredentialsServiceProvider;
import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.support.JsonHelper;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import lombok.*;
import lombok.extern.slf4j.*;
import org.apache.camel.Exchange;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Slf4j
@Data
@RuleNode(type = RuleNodeType.ACTION, value = "email", title = "Email sender", description = "Email sender")
public class EmailComponent implements ProcessorNode<Exchange> {

    private static final ExecutorService SHARED_SMTP_EXECUTOR = new ThreadPoolExecutor(1, 20, 300L, TimeUnit.SECONDS, new LinkedBlockingQueue<>(1000));

    @Autowired
    private CredentialsServiceProvider credentialsServiceProvider;

    @Metadata(required = true)
    @UriParamExtension(uiComponentGroup = "Email Notification", uiComponent = "text")
    @UriParam(displayName = "Subject", description = "Email subject.", prefix = "bean")
    private String subject;

    @Metadata(/* Hidden from UI */ autowired = true)
    @UriParamExtension(uiComponentGroup = "Email Notification", uiComponent = "text")
    @UriParam(displayName = "From Name", description = "Email from name.", prefix = "bean")
    private String fromName;

    @Metadata(/* Hidden from UI */ autowired = true)
    @UriParamExtension(uiComponentGroup = "Email Notification", uiComponent = "text")
    @UriParam(displayName = "From Address", description = "Email from address.", prefix = "bean")
    private String fromAddress;

    @Metadata(required = true)
    @UriParamExtension(uiComponentGroup = "Email Notification", uiComponent = "emailRecipients")
    @UriParam(displayName = "Recipients", description = "Email recipients.", prefix = "bean", javaType = "java.util.List<java.lang.String>")
    private List<String> recipients;

    @UriParamExtension(uiComponentGroup = "Email Notification", uiComponent = "emailContent", loggable = true)
    @UriParam(displayName = "Content", description = "Email content.", prefix = "bean")
    private String content;

    @UriParamExtension(uiComponent = "emailSendSource")
    @UriParam(displayName = "Email Settings", prefix = "bean", javaType = "com.milesight.beaveriot.rule.components.email.EmailConfig")
    private EmailConfig emailConfig;

    private final Object lock = new Object();

    private EmailChannel initEmailChannel() {
        if (emailConfig == null) {
            throw new IllegalArgumentException("Email config is null.");
        }

        EmailConfig.SmtpConfig smtpConfig = null;
        if (EmailProvider.SMTP.equals(emailConfig.getProvider())
                && emailConfig.getSmtpConfig() != null) {
            smtpConfig = emailConfig.getSmtpConfig();
        } else if (Boolean.TRUE.equals(emailConfig.getUseSystemSettings())) {
            val credentials = credentialsServiceProvider.getCredentials(CredentialsType.SMTP)
                    .orElseThrow(() -> new ServiceException(ErrorCode.DATA_NO_FOUND, "credentials not found"));
            smtpConfig = JsonUtils.fromJSON(credentials.getAdditionalData(), EmailConfig.SmtpConfig.class);
            if (smtpConfig == null || !StringUtils.hasText(smtpConfig.getUsername())) {
                throw new ServiceException(ErrorCode.DATA_NO_FOUND, "system smtp config not found");
            }
            if (smtpConfig.getPassword() == null) {
                smtpConfig.setPassword(credentials.getAccessSecret());
            }
        }

        if (smtpConfig != null) {
            return new SmtpChannel(smtpConfig, SHARED_SMTP_EXECUTOR);
        } else {
            throw new IllegalArgumentException("Email provider is not supported or config is null: " + emailConfig.getProvider());
        }

    }

    @Override
    public void processor(Exchange exchange) {
        val emailChannel = initEmailChannel();
        var templates = Map.<String, Object>of("subject", subject, "content", content);
        var outputs = SpELExpressionHelper.resolveExpression(exchange, templates);
        emailChannel.send(fromName, fromAddress, recipients, (String) outputs.get("subject"), (String) outputs.get("content"));
    }

    public void setEmailConfig(String json) {
        emailConfig = JsonHelper.fromJSON(json, EmailConfig.class);
    }
}
