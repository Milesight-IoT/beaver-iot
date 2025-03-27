package com.milesight.beaveriot.mqtt.controller;

import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.context.mqtt.model.MqttBrokerInfo;
import com.milesight.beaveriot.mqtt.model.WebMqttCredentials;
import com.milesight.beaveriot.mqtt.service.MqttAclService;
import com.milesight.beaveriot.mqtt.service.MqttPubSubService;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequestMapping("/mqtt")
public class MqttController {

    @Autowired
    private MqttPubSubService mqttPubSubService;

    @Autowired
    private MqttAclService mqttAclService;

    @GetMapping("/broker-info")
    public ResponseBody<MqttBrokerInfo> getMqttBrokerInfo() {
        return ResponseBuilder.success(mqttPubSubService.getMqttBrokerInfo());
    }


    @GetMapping("/web/credentials")
    public ResponseBody<WebMqttCredentials> getWebMqttCredentials() {
        return ResponseBuilder.success(mqttAclService.getOrInitWebMqttCredentials());
    }

}
