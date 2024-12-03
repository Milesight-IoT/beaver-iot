package com.milesight.beaveriot.dashboard.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.util.Map;

/**
 * @author loong
 * @date 2024/10/18 9:46
 */
@Data
public class DashboardWidgetDTO implements Serializable {

    @JsonIgnore
    private Long dashboardId;
    private String widgetId;
    private Map<String, Object> data;

}
