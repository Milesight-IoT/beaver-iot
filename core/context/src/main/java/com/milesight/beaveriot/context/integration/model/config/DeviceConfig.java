package com.milesight.beaveriot.context.integration.model.config;

import com.milesight.beaveriot.context.integration.model.Entity;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author leon
 */
@Data
public class DeviceConfig {

    private String name;
    private Map<String, Object> additional;
    private String identifier;
    private List<EntityConfig> entities = new ArrayList<>();

}
