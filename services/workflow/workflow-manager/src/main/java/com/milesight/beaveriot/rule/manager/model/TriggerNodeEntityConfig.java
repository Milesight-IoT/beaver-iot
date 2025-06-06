package com.milesight.beaveriot.rule.manager.model;

import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import lombok.Data;

/**
 * TriggerNodeEntityConfig class.
 *
 * @author simon
 * @date 2025/5/26
 */
@Data
public class TriggerNodeEntityConfig {
    private String identify;
    private String name;
    private EntityValueType type;
    private Boolean required;
}
