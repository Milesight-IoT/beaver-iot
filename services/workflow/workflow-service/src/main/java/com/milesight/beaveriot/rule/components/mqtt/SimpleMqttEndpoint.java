package com.milesight.beaveriot.rule.components.mqtt;


import com.fasterxml.jackson.core.type.TypeReference;
import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import com.milesight.beaveriot.rule.model.OutputVariablesSettings;
import com.milesight.beaveriot.rule.support.JsonHelper;
import lombok.*;
import lombok.extern.slf4j.*;
import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

import java.util.List;

@Slf4j
@UriEndpoint(firstVersion = "4.4.3", scheme = "simpleMqtt", title = "MQTT", syntax = "simpleMqtt:flowId",
        remote = false, consumerOnly = true, category = {Category.MESSAGING})
public class SimpleMqttEndpoint extends DefaultEndpoint {

    @Setter
    @Getter
    @UriPath
    @Metadata(required = true, autowired = true)
    private String flowId;

    @Setter
    @Getter
    @UriParamExtension(uiComponent = "text")
    @Metadata(required = true, defaultValue = "#")
    @UriParam(displayName = "Subscription Topic", description = "MQTT Subscription Topic")
    private String subscriptionTopic;

    @Setter
    @Getter
    @UriParamExtension(uiComponent = "credentials")
    @Metadata
    @UriParam(displayName = "Credentials", description = "Credentials")
    private String credentialsId;

    @Setter
    @Getter
    @UriParamExtension(uiComponent = "text")
    @Metadata(required = true)
    @UriParam(displayName = "Encoding", description = "MQTT Payload Encoding", enums = "UTF8,BASE64")
    private String encoding;

    @OutputArguments(displayName = "Output Variables")
    @UriParamExtension(uiComponent = "paramDefineInput")
    @UriParam(displayName = "Output Variables", description = "Received MQTT message.", defaultValue = "[{\"name\":\"topic\",\"type\":\"STRING\"},{\"name\":\"payload\",\"type\":\"STRING\"}]")
    private List<OutputVariablesSettings> message;

    public SimpleMqttEndpoint() {
        super();
    }

    public SimpleMqttEndpoint(String uri, String flowId, SimpleMqttComponent component) {
        super(uri, component);
        this.flowId = flowId;
        log.info("MqttEndpoint created");
    }

    public void setMessage(String json) {
        //noinspection Convert2Diamond
        message = JsonHelper.fromJSON(json, new TypeReference<List<OutputVariablesSettings>>() {
        });
    }

    @Override
    public Producer createProducer() throws Exception {
        return null;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        log.info("MqttEndpoint createConsumer");
        return new SimpleMqttConsumer(this, processor);
    }

    @Override
    protected void doStop() throws Exception {
        log.info("MqttEndpoint stop");
    }

    @Override
    public boolean equals(Object object) {
        return super.equals(object);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
