package com.milesight.beaveriot.sample.rule.node;

import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.support.JsonHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 *   "timerName": { "index": 0, "kind": "path", "displayName": "Timer Name", "group": "consumer", "label": "", "required": true, "type": "string", "javaType": "java.lang.String", "deprecated": false, "deprecationNote": "", "autowired": false, "secret": false, "description": "The name of the timer" },
 *     "delay": { "index": 1, "kind": "parameter", "displayName": "Delay", "group": "consumer", "label": "", "required": false, "type": "duration", "javaType": "long", "deprecated": false, "autowired": false, "secret": false, "defaultValue": "1000", "description": "Delay before first event is triggered." },
 * @author leon
 */
@Slf4j
@Data
@Component
@RuleNode(type = RuleNodeType.ACTION)
//@UriEndpoint(firstVersion = "1.0.0", scheme = "eventbus", title = "SimpleAlarmComponent", syntax = "eventbus:eventbusName", consumerOnly = true)
public class SimpleAlarmComponent  {

    @UriPath
    @Metadata(required = true)
    private String eventbusName;
    @Metadata(required = false)
    private String status;

    @UriParam(javaType = "Entity")   //note: 增加  string -> Entity的Converter
    private Entity serviceEntity;

    @UriParamExtension( uiComponent = "text")
    @UriParam(javaType = "Entity", prefix = "bean")
    private Entity propertyEntity;

    @OutputArguments
    //  ${constant}   ${request.body.xxx}    ${request.header.xxxx}  ${bean:entityValueFinder}
    @UriParamExtension( uiComponent = "entitySelect"
//            valueProvider = { ValueExpression.CONSTANT,  ValueExpression.CAMEL_BODY,ValueExpression.CAMEL_HEADER,ValueExpression.SPRING_BEAN}
            )
    @UriParam(/*label = "expression",*/ javaType = "exchangePayload", prefix = "bean")
    private String exchangePayload;

    @OutputArguments
    @UriParam(/*label = "expression",*/ javaType = "string", prefix = "bean")
    private List<String> entities;

    @OutputArguments
    //label : 可用于描述输入框类型    javaType : 用于描述输入框的数据类型（结合前端组件）  enums : 用于描述输入框的值类型（expression特定情况下分下拉框）
    @UriParamExtension(uiComponent = "emailText")
    @UriParam( javaType = "exchangeValue", prefix = "bean")
    private String emailContent ="${}";

    @OutputArguments
    @UriParamExtension(uiComponent = "entitySelectWithExchange"/*, valueProvider = {ValueExpression.CAMEL_BODY}*/)
    @UriParam( javaType = "string", prefix = "bean")
    private String webhookContent;

    @OutputArguments
    @UriParam(label = "advanced", prefix = "bean")
    private boolean includeMetadata = false;  //note: include metadata by default

    @OutputArguments
    @UriParam(prefix = "bean")
    private int size;

    @OutputArguments(name = "body", description = "The body of the message", javaType = "ExchangePayload")
    private String aaa;

    public void process(Exchange exchange) throws Exception {
        log.info("SimpleAlarmComponent process...................." + emailContent);
        exchange.getIn().setBody(JsonHelper.fromJSON(exchangePayload, Map.class));
    }

}
