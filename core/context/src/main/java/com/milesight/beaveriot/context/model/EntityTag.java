package com.milesight.beaveriot.context.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityTag {
    private String id;
    private String name;
    private String description;
    private String color;
}
