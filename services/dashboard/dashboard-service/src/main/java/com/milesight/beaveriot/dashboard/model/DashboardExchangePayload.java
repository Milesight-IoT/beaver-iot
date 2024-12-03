package com.milesight.beaveriot.dashboard.model;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * @author loong
 * @date 2024/11/1 8:55
 */
@Data
public class DashboardExchangePayload implements Serializable {

    private List<String> entityKey;

}
