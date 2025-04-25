package com.milesight.beaveriot.rule.components.timer;

import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
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

@Slf4j
@UriEndpoint(firstVersion = "4.4.3", scheme = "simpleTimer", title = "SimpleTimer", syntax = "simpleTimer:flowId",
        remote = false, consumerOnly = true, category = {Category.SCHEDULING})
@RuleNode(type = RuleNodeType.ENTRY, value = "simpleTimer", testable = false)
public class SimpleTimerEndpoint extends DefaultEndpoint {

    @Setter
    @Getter
    @UriPath
    @Metadata(required = true, autowired = true)
    private String flowId;

    @Getter
    @UriParamExtension(uiComponent = "timerSettings")
    @UriParam(displayName = "Timer Settings", description = "Timer settings.")
    private SimpleTimerSettings timerSettings;

    public SimpleTimerEndpoint() {

    }

    public SimpleTimerEndpoint(String uri, String flowId, SimpleTimerComponent component) {
        setEndpointUri(uri);
        setFlowId(flowId);
        setComponent(component);
        setCamelContext(component.getCamelContext());
        log.info("SimpleTimerEndpoint created");
    }

    public void setTimerSettings(String json) {
        timerSettings = JsonHelper.fromJSON(json, SimpleTimerSettings.class);
    }

    @Override
    public Producer createProducer() throws Exception {
        return null;
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        log.info("SimpleTimerEndpoint createConsumer");
        return new SimpleTimerConsumer(this, processor);
    }

    @Override
    protected void doStop() throws Exception {
        log.info("SimpleTimerEndpoint stop");
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
