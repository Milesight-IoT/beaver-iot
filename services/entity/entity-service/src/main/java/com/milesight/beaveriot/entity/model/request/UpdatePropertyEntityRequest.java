package com.milesight.beaveriot.entity.model.request;

import lombok.Data;

import java.util.Map;

/**
 * @author loong
 * @date 2024/10/17 9:17
 */
@Data
public class UpdatePropertyEntityRequest {

    private Map<String, Object> exchange;

}
