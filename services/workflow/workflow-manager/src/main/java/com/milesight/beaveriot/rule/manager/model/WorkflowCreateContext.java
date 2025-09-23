package com.milesight.beaveriot.rule.manager.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WorkflowCreateContext class.
 *
 * @author simon
 * @date 2025/9/23
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WorkflowCreateContext {
    private Long deviceId;
}
