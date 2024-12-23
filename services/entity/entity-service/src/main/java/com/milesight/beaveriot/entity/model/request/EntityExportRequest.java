package com.milesight.beaveriot.entity.model.request;

import lombok.*;

import java.util.List;

/**
 * The request body for exporting entity data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EntityExportRequest {

    /**
     * The IDs of the entities to be exported.
     */
    private List<Long> entityIds;

    /**
     * The start timestamp of the data to be exported. <br>
     * Unix timestamp in milliseconds. <br>
     * Default: 0
     */
    private Long startTimestamp;

    /**
     * The end timestamp of the data to be exported. <br>
     * Unix timestamp in milliseconds. <br>
     * Default: current timestamp
     */
    private Long endTimestamp;

}