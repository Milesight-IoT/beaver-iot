package com.milesight.beaveriot.entity.model.dto;

import com.milesight.beaveriot.base.enums.ComparisonOperator;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.lang.Nullable;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityAdvancedSearchCondition {

    @NotNull
    private ComparisonOperator operator;

    @Nullable
    @Size(min = 1, max = 100)
    private List<String> values;

}
