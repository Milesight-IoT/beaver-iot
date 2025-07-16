package com.milesight.beaveriot.entity.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityTagUpdateRequest {

    @NotBlank
    @Size(min = 1, max = 25)
    private String name;

    @Size(max = 63)
    private String description;

    @NotBlank
    @Size(min = 1, max = 32)
    private String color;

}
