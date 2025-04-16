package com.milesight.beaveriot.resource.adapter.db.service.model;

import lombok.Data;
import lombok.experimental.FieldNameConstants;

/**
 * DbResourceDataPreSignPO class.
 *
 * @author simon
 * @date 2025/4/12
 */
@Data
@FieldNameConstants
public class DbResourceDataPreSignData {
    private String objKey;

    private Long expiredAt;
}
