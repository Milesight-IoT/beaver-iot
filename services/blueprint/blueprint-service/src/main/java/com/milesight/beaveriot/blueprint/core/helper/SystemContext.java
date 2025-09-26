package com.milesight.beaveriot.blueprint.core.helper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemContext {

    private Long userId;

    private String tenantId;

}
