package com.milesight.beaveriot.entity.model.response;

import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityTagResponse {

    private String id;

    private String name;

    private String description;

    private String color;

    private Long createdAt;

    private Long updatedAt;

    private Long taggedEntitiesCount;

}
