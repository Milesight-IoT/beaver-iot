package com.milesight.beaveriot.entity.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import com.milesight.beaveriot.entity.enums.EntitySearchColumn;
import com.milesight.beaveriot.entity.model.dto.EntityAdvancedSearchCondition;
import jakarta.validation.Valid;
import lombok.*;

import java.util.Map;

@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityAdvancedSearchQuery extends GenericPageRequest {

    private Map<EntitySearchColumn, @Valid EntityAdvancedSearchCondition> entityFilter;

}
