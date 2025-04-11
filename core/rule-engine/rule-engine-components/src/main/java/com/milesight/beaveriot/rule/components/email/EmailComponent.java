package com.milesight.beaveriot.rule.components.email;

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

import java.util.List;
import java.util.Map;


@Slf4j
@Data
@RuleNode(type = RuleNodeType.ACTION, value = "email", title = "Email sender", description = "Email sender")
public class EmailComponent implements ProcessorNode<Exchange> {

    @UriParamExtension(uiComponentGroup = "emailContent", uiComponent = "text")
    @UriParam(displayName = "Subject", description = "Email subject.", prefix = "bean")
    private String subject;

    @Metadata(/* Hidden from UI */ autowired = true)
    @UriParamExtension(uiComponentGroup = "emailContent", uiComponent = "text")
    @UriParam(displayName = "From Name", description = "Email from name.", prefix = "bean")
    private String fromName;

    @Metadata(/* Hidden from UI */ autowired = true)
    @UriParamExtension(uiComponentGroup = "emailContent", uiComponent = "text")
    @UriParam(displayName = "From Address", description = "Email from address.", prefix = "bean")
    private String fromAddress;

    @UriParamExtension(uiComponentGroup = "emailContent", uiComponent = "emailRecipients")
    @UriParam(displayName = "Recipients", description = "Email recipients.", prefix = "bean", javaType = "java.util.List<java.lang.String>")
    private List<String> recipients;

    @UriParamExtension(uiComponentGroup = "emailContent", uiComponent = "emailContent", loggable = true)
    @UriParam(displayName = "Content", description = "Email content.", prefix = "bean")
    private String content;

    @UriParamExtension(uiComponent = "emailSendSource")
    @UriParam(displayName = "Email Settings", prefix = "bean", javaType = "com.milesight.beaveriot.rule.components.email.EmailConfig")
    private EmailConfig emailConfig;

    private final Object lock = new Object();

    private EmailChannel emailChannel;

    private void initEmailChannelIfNotExists() {
        if (emailChannel != null) {
            return;
        }
        if (emailConfig == null) {
            throw new IllegalArgumentException("Email config is null.");
        }
        synchronized (lock) {
            if (emailChannel != null) {
                return;
            }
            if (EmailProvider.SMTP.equals(emailConfig.getProvider())
                    && emailConfig.getSmtpConfig() != null) {
                emailChannel = new SmtpChannel(emailConfig.getSmtpConfig());
            } else {
                throw new IllegalArgumentException("Email provider is not supported or config is null: " + emailConfig.getProvider());
            }
        }
    }

    @Override
    public void processor(Exchange exchange) {
        initEmailChannelIfNotExists();
        if (emailChannel == null) {
            log.warn("Email channel is not ready.");
            return;
        }
        var templates = Map.<String, Object>of("subject", subject, "content", content);
        var outputs = SpELExpressionHelper.resolveExpression(exchange, templates);
        emailChannel.send(fromName, fromAddress, recipients, (String) outputs.get("subject"), (String) outputs.get("content"));
    }

    public void setEmailConfig(String json) {
        emailConfig = JsonHelper.fromJSON(json, EmailConfig.class);
    }
}
