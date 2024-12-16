package com.milesight.beaveriot.rule.components.eventlistener;

import com.milesight.beaveriot.context.integration.model.Entity;
import lombok.Data;
import org.apache.camel.*;
import org.apache.camel.api.management.ManagedResource;
import org.apache.camel.spi.Metadata;
import org.apache.camel.spi.UriEndpoint;
import org.apache.camel.spi.UriParam;
import org.apache.camel.spi.UriPath;
import org.apache.camel.support.DefaultEndpoint;

import java.util.List;

@Data
@ManagedResource(description = "Managed EventBusEndpoint")
@UriEndpoint(firstVersion = "1.0.0", scheme = "eventlistener", title = "Event Listener", syntax = "eventlistener:eventListenerName", consumerOnly = true,
        remote = false, category = { Category.WORKFLOW}, headersClass = EventListenerConstants.class)
public class EventListenerEndpoint extends DefaultEndpoint {

    @UriPath
    @Metadata(required = true, autowired = true)
    private String eventListenerName;

    @UriParam(displayName = "Entity Listening Setting", description = "The entities to listen for events")
    private List<String> entities;

    @Metadata(autowired = true)
    @UriParam(defaultValue = "false", label = "advanced", description = "Whether to process the exchange synchronously or asynchronously.")
    private boolean synchronous = false;

    public EventListenerEndpoint(String uri, EventListenerComponent component) {
        super(uri, component);
    }

    @Override
    public Producer createProducer() throws Exception {
        throw new UnsupportedOperationException("EventListenerEndpoint does not support producers");
    }

    @Override
    public Consumer createConsumer(Processor processor) throws Exception {
        return new EventListenerConsumer(this, processor);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}