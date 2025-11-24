package com.milesight.beaveriot.entity.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Lightweight DTO for entity ID and key (used for validation)
 *
 * @author simon
 * @date 2025/11/21
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityIdKeyDTO {
  private Long id;
  private String key;
}
