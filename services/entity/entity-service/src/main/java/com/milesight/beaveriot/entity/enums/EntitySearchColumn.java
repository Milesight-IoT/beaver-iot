package com.milesight.beaveriot.entity.enums;

import com.milesight.beaveriot.base.enums.ComparisonOperator;
import com.milesight.beaveriot.entity.po.EntityPO;
import lombok.*;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


@Getter
@RequiredArgsConstructor
public enum EntitySearchColumn {
    ENTITY_KEY(EntityPO.Fields.key, Set.of(ComparisonOperator.CONTAINS, ComparisonOperator.NOT_CONTAINS, ComparisonOperator.EQ, ComparisonOperator.NE, ComparisonOperator.START_WITH, ComparisonOperator.END_WITH)),
    ENTITY_NAME(EntityPO.Fields.name, Set.of(ComparisonOperator.CONTAINS, ComparisonOperator.NOT_CONTAINS, ComparisonOperator.EQ, ComparisonOperator.NE, ComparisonOperator.START_WITH, ComparisonOperator.END_WITH)),
    ENTITY_TYPE(EntityPO.Fields.type, Set.of(ComparisonOperator.ANY_EQUALS)),
    ENTITY_VALUE_TYPE(EntityPO.Fields.valueType, Set.of(ComparisonOperator.ANY_EQUALS)),
    DEVICE_NAME(Set.of(ComparisonOperator.CONTAINS, ComparisonOperator.NOT_CONTAINS, ComparisonOperator.EQ, ComparisonOperator.NE, ComparisonOperator.START_WITH, ComparisonOperator.END_WITH)),
    INTEGRATION_NAME(Set.of(ComparisonOperator.CONTAINS, ComparisonOperator.NOT_CONTAINS, ComparisonOperator.EQ, ComparisonOperator.NE, ComparisonOperator.START_WITH, ComparisonOperator.END_WITH)),
    ENTITY_PARENT_NAME(Set.of(ComparisonOperator.CONTAINS, ComparisonOperator.NOT_CONTAINS, ComparisonOperator.EQ, ComparisonOperator.NE, ComparisonOperator.START_WITH, ComparisonOperator.END_WITH, ComparisonOperator.IS_EMPTY, ComparisonOperator.IS_NOT_EMPTY)),
    ENTITY_TAGS(Set.of(ComparisonOperator.ANY_EQUALS, ComparisonOperator.EQ, ComparisonOperator.CONTAINS, ComparisonOperator.NOT_CONTAINS, ComparisonOperator.IS_EMPTY, ComparisonOperator.IS_NOT_EMPTY)),
    DEVICE_GROUP(Set.of(ComparisonOperator.ANY_EQUALS)),
    DEVICE_ID(Set.of(ComparisonOperator.EQ)),
    ;

    private static final Map<String, EntitySearchColumn> NAME_TO_ENUM = Arrays.stream(EntitySearchColumn.values())
            .collect(Collectors.toMap(k -> k.name().toLowerCase(), v -> v));

    private final String columnName;

    private final Set<ComparisonOperator> supportedOperators;

    EntitySearchColumn(Set<ComparisonOperator> supportedOperators) {
        this.columnName = null;
        this.supportedOperators = supportedOperators;
    }

    @Override
    public String toString() {
        return name();
    }

    public static EntitySearchColumn fromString(String name) {
        return NAME_TO_ENUM.get(name.toLowerCase());
    }

}
