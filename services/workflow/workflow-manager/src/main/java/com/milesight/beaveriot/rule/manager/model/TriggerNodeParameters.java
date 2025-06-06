package com.milesight.beaveriot.rule.manager.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class TriggerNodeParameters {
    private List<TriggerNodeEntityConfig> entityConfigs;
}
