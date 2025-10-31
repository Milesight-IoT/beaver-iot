package com.milesight.beaveriot.context.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SearchDeviceTemplateRequest extends GenericPageRequest {
    private String name;
    private List<Long> deviceTemplateIds;
    private DeviceTemplateSource deviceTemplateSource = DeviceTemplateSource.ALL;

    public enum DeviceTemplateSource {
        ALL,
        CUSTOM,
        BLUEPRINT_LIBRARY
    }
}