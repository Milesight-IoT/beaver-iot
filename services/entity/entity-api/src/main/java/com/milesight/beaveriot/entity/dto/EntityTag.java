package com.milesight.beaveriot.entity.dto;

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
