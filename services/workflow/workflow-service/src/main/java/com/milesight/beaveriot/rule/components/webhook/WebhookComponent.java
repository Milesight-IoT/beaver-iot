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
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Random;

/**
 * @author loong
 * @date 2024/12/17 15:56
 */
@RuleNode(value = "webhook", type = RuleNodeType.EXTERNAL, description = "Webhook")
@Data
public class WebhookComponent implements ProcessorNode<Exchange> {

    @UriParam(prefix = "bean", displayName = "Payload")
    @UriParamExtension(uiComponent = "paramAssignInput", loggable = true)
    private Map<String, Object> inputArguments;
    @UriParam(prefix = "bean")
    private String webhookUrl;
    @UriParam(prefix = "bean", description = "Header.Signature = HmacSha256(Header.Timestamp + Header.Nonce + Body)")
    private String secretKey;

    @Autowired
    private ProducerTemplate producerTemplate;

    @Override
    public void processor(Exchange exchange) {
        if(inputArguments != null && !inputArguments.isEmpty()) {
            Map<String, Object> inputArgumentsVariables = SpELExpressionHelper.resolveExpression(exchange, inputArguments);
            exchange.getIn().setBody(inputArgumentsVariables);
        }
        Object bodyObject = exchange.getIn().getBody();
        String body = JsonUtils.toJSON(bodyObject);
        if(StringUtils.hasText(secretKey)) {
            String timestamp = System.currentTimeMillis() + "";
            Random random = new Random();
            int randomNumber = random.nextInt(99999999) + 10000000;
            String nonce = randomNumber + "";
            String data = timestamp + nonce + body;
            String signature = SecureUtil.hmacSha256Hex(secretKey, data);

            exchange.getIn().setHeader("TIMESTAMP", timestamp);
            exchange.getIn().setHeader("NONCE", nonce);
            exchange.getIn().setHeader("SIGNATURE", signature);
        }
        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/json;charset=UTF-8");
        producerTemplate.sendBodyAndHeaders(webhookUrl, body, exchange.getIn().getHeaders());
    }

}
