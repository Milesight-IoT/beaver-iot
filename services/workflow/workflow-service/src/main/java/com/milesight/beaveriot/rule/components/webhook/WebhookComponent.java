package com.milesight.beaveriot.rule.components.webhook;

import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.api.ProcessorNode;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.support.SpELExpressionHelper;
import com.milesight.beaveriot.rule.util.SecureUtil;
import lombok.Data;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.spi.UriParam;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.Random;

/**
 * @author loong
 * @date 2024/12/17 15:56
 */
@RuleNode(value = "webhook", type = RuleNodeType.EXTERNAL, description = "Webhook")
@Data
public class WebhookComponent implements ProcessorNode<Exchange> {

    @UriParam(prefix = "bean")
    @UriParamExtension(uiComponent = "paramAssignInput")
    private Map<String, Object> payload;
    @UriParam(prefix = "bean")
    private String webhookUrl;
    @UriParam(prefix = "bean")
    private String secretKey;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Override
    public void processor(Exchange exchange) {
        if(payload != null && !payload.isEmpty()) {
            Map<String, Object> inputArgumentsVariables = SpELExpressionHelper.resolveExpression(exchange, payload);
            exchange.getIn().setBody(inputArgumentsVariables);
        }
        String timestamp = System.currentTimeMillis() + "";
        Random random = new Random();
        int randomNumber = random.nextInt(99999999) + 10000000;
        String nonce = randomNumber + "";
        Object bodyObject = exchange.getIn().getBody();
        String body = JsonUtils.toJSON(bodyObject);
        String data = timestamp + nonce + body;
        String signature = SecureUtil.hmacSha256Hex(secretKey, data);

        exchange.getIn().setHeader("TIMESTAMP", timestamp);
        exchange.getIn().setHeader("NONCE", nonce);
        exchange.getIn().setHeader("SIGNATURE", signature);
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json");
        producerTemplate.sendBodyAndHeaders(webhookUrl, body, exchange.getIn().getHeaders());
    }

}
