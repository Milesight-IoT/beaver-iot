package com.milesight.beaveriot.user.model.request;

import lombok.Data;

import java.util.List;

/**
 * @author loong
 * @date 2024/12/27 17:26
 */
@Data
public class BatchDeleteUserRequest {

    private List<Long> userIdList;

}
