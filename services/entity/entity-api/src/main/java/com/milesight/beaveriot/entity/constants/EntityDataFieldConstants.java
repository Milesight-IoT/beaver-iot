package com.milesight.beaveriot.entity.constants;

import com.milesight.beaveriot.context.integration.model.AttributeBuilder;

import java.util.List;
import java.util.Set;

/**
 * EntityDataFieldConstants class.
 *
 * @author simon
 * @date 2025/5/22
 */
public class EntityDataFieldConstants {
    private EntityDataFieldConstants() {}

    public static final int ENTITY_NAME_MAX_LENGTH = 64;

    public static final int CUSTOM_ENTITY_IDENTIFIER_MAX_LENGTH = 50;

    public static final int CUSTOM_ENTITY_ENUM_STRING_MAX_LENGTH = 25;

    public static final int CUSTOM_ENTITY_ENUM_MAX_SIZE = 10;

    public static final int CUSTOM_ENTITY_UNIT_STRING_MAX_LENGTH = 15;

    public static final String CUSTOM_ENTITY_ATTRIBUTE_IS_ENUM = "is_enum";

    public static final Set<String> CUSTOM_ENTITY_ALLOWED_ATTRIBUTES = Set.of(
            AttributeBuilder.ATTRIBUTE_OPTIONAL,    // for workflow trigger
            AttributeBuilder.ATTRIBUTE_ENUM,
            AttributeBuilder.ATTRIBUTE_UNIT,
            AttributeBuilder.ATTRIBUTE_MIN_LENGTH,
            AttributeBuilder.ATTRIBUTE_MAX_LENGTH,
            AttributeBuilder.ATTRIBUTE_MIN,
            AttributeBuilder.ATTRIBUTE_MAX,
            CUSTOM_ENTITY_ATTRIBUTE_IS_ENUM
    );
}
