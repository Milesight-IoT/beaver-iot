package com.milesight.beaveriot.entity.model.dto;

import com.milesight.beaveriot.entity.exporter.ExportField;
import lombok.*;

/**
 * Export data of entity
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityExportData {

    /**
     * Entity name
     */
    @ExportField(header = "Entity Name")
    private String entityName;

    /**
     * Entity identifier
     */
    @ExportField(header = "Entity Identifier")
    private String entityIdentifier;

    /**
     * Entity value
     */
    @ExportField(header = "Value")
    private String value;

    /**
     * Entity update time<br>
     * format: yyyy/MM/dd HH:mm:ss
     */
    @ExportField(header = "Update Time")
    private String updateTime;

    /**
     * Integration name
     */
    @ExportField(header = "Integration")
    private String integrationName;

    /**
     * Device name
     */
    @ExportField(header = "Device")
    private String deviceName;

}
