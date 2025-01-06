package com.milesight.beaveriot.rule.components.timer;

import lombok.extern.slf4j.*;
import org.apache.camel.CamelContext;
import org.apache.camel.Endpoint;
import org.apache.camel.ExtendedStartupListener;
import org.apache.camel.spi.annotations.Component;
import org.apache.camel.support.DefaultComponent;

import java.util.Map;


@Slf4j
@Component("simpleTimer")
public class SimpleTimerComponent extends DefaultComponent implements ExtendedStartupListener {

    public SimpleTimerComponent(CamelContext camelContext) {
        super(camelContext);
    }

    @Override
    protected Endpoint createEndpoint(String uri, String remaining, Map<String, Object> parameters) throws Exception {
        var endpoint = new SimpleTimerEndpoint(uri, remaining, this);
        setProperties(endpoint, parameters);
        return endpoint;
    }

    @Override
    public void onCamelContextStarted(CamelContext context, boolean alreadyStarted) throws Exception {
        if (alreadyStarted) {
            log.info("camel context started");
        }
    }

    @Override
    public void onCamelContextFullyStarted(CamelContext context, boolean alreadyStarted) throws Exception {
        log.info("camel context full started");
    }

    @Override
    protected void doStop() throws Exception {
        log.info("do stop");
        SimpleTimerScheduler.instance().clear();
    }
}
