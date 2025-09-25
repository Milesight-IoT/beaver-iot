package com.milesight.beaveriot.rule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WorkflowNameDTO class.
 *
 * @author simon
 * @date 2025/9/25
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WorkflowNameDTO {
    private Long workflowId;

    private Long entityId;

    private String name;
}
