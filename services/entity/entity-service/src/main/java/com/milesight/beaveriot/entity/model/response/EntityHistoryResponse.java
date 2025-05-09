package com.milesight.beaveriot.entity.model.response;

import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import lombok.*;

/**
 * @author loong
 * @date 2024/10/21 10:51
 */
@Data
public class EntityHistoryResponse {

    private String id;

    private String entityId;

    private String timestamp;

    private Object value;

    private EntityValueType valueType;

}
