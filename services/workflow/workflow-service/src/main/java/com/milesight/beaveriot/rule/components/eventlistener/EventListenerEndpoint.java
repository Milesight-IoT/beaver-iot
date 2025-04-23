package com.milesight.beaveriot.rule.components.eventlistener;

import com.milesight.beaveriot.rule.annotations.OutputArguments;
import com.milesight.beaveriot.rule.annotations.RuleNode;
import com.milesight.beaveriot.rule.annotations.UriParamExtension;
import com.milesight.beaveriot.rule.constants.RuleNodeType;
import lombok.Data;
import org.apache.camel.Category;
import org.apache.camel.Consumer;
import org.apache.camel.Processor;
import org.apache.camel.Producer;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

import java.util.List;

@RuleNode(type = RuleNodeType.ENTRY, value = "eventListener", testable = false)
@Data
@ManagedResource(description = "Managed EventBusEndpoint")
@UriEndpoint(firstVersion = "1.0.0", scheme = "eventListener", title = "Event Listener", syntax = "eventListener:eventListenerName", consumerOnly = true,
        remote = false, category = { Category.WORKFLOW}, headersClass = EventListenerConstants.class)
public class EventListenerEndpoint extends DefaultEndpoint {


    @UriPath
    @Metadata(required = true, autowired = true)
    private String eventListenerName;
    @UriParamExtension(uiComponent = "entityMultipleSelect")
    @UriParam(displayName = "Entity Listening Setting", description = "The entities to listen for events")
    @OutputArguments
    private List<String> entities;

    @Metadata(required = true, autowired = true, defaultValue = "false")
    @UriParam(displayName = "verifyEntitiesValidation", description = "Whether it is necessary to verify the legality of the entity")
    private boolean verifyEntitiesValidation = false;

    public EventListenerEndpoint(String uri, String eventListenerName, EventListenerComponent component) {
        super(uri, component);
        this.eventListenerName = eventListenerName;
    }
    public EventListenerEndpoint() {
    }

    @Override
    public Producer createProducer() throws Exception {
        throw new UnsupportedOperationException("EventListenerEndpoint does not support producers");
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        EventListenerComponent eventListenerComponent = (EventListenerComponent) getComponent();
        return new EventListenerConsumer(this, processor, eventListenerComponent.getGenericExchangeValidator(), eventListenerComponent.getEventBus());
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}