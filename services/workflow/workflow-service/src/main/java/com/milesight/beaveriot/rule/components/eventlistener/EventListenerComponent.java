package com.milesight.beaveriot.rule.components.eventlistener;

import com.milesight.beaveriot.entity.rule.GenericExchangeValidator;
import com.milesight.beaveriot.eventbus.EventBusDispatcher;
import com.milesight.beaveriot.rule.constants.RuleNodeNames;
import org.apache.camel.BeanInject;
import org.apache.camel.Endpoint;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;

@org.apache.camel.spi.annotations.Component("eventListener")
public class EventListenerComponent extends DefaultComponent {

    private Map<String,Object> parameters;
    @BeanInject(RuleNodeNames.innerExchangeValidator)
    private GenericExchangeValidator genericExchangeValidator;
    @BeanInject
    private EventBusDispatcher eventBus;
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        this.parameters = parameters;
        EventListenerEndpoint endpoint = new EventListenerEndpoint(uri, remaining, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }

    public EventBusDispatcher getEventBus() {
        return eventBus;
    }

    public GenericExchangeValidator getGenericExchangeValidator() {
        return genericExchangeValidator;
    }
}