package com.milesight.beaveriot.context.integration.entity;

import com.milesight.beaveriot.context.integration.model.Integration;
import org.springframework.core.env.StandardEnvironment;

/**
 * @author leon
 */
public interface EntityLoader {

    void load(Integration integration, StandardEnvironment integrationEnvironment);

}
