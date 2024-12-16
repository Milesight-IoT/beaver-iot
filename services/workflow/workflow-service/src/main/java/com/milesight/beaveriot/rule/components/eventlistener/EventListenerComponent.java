package com.milesight.beaveriot.rule.components.eventlistener;

import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;

@org.apache.camel.spi.annotations.Component("eventlistener")
public class EventListenerComponent extends DefaultComponent {

    private Map<String,Object> parameters;

    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        this.parameters = parameters;
        EventListenerEndpoint endpoint = new EventListenerEndpoint(uri, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }

}