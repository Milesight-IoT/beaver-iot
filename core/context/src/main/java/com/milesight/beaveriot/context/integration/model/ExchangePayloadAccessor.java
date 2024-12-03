package com.milesight.beaveriot.context.integration.model;


import com.milesight.beaveriot.context.integration.enums.EntityType;

import java.util.Map;

/**
 * @author leon
 */
public interface ExchangePayloadAccessor {

    Object getPayload(String key);

    Map<String, Object> getAllPayloads();

    Map<String, Object> getPayloadsByEntityType(EntityType entityType);

    Map<String, Entity> getExchangeEntities();

}
