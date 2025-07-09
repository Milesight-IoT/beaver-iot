package com.milesight.beaveriot.entity.model.request;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityTagDeleteRequest {

    private List<Long> ids;

}
