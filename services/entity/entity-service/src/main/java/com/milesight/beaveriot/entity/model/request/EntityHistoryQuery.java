package com.milesight.beaveriot.entity.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/10/16 15:01
 */
@Data
public class EntityHistoryQuery extends GenericPageRequest {

    private Long entityId;
    private List<Long> entityIds;
    private Long startTimestamp;
    private Long endTimestamp;

}
