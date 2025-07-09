package com.milesight.beaveriot.entity.model.request;

import com.milesight.beaveriot.entity.enums.EntityTagMappingOperation;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityTagMappingRequest {

    @NotNull
    EntityTagMappingOperation operation;

    @NotEmpty
    List<Long> entityIds;

    @Size(min = 1, max = 300)
    List<Long> removedTagIds;

    @Size(min = 1, max = 10)
    List<Long> addedTagIds;

}
