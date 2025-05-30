package com.milesight.beaveriot.context.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SearchDeviceTemplateRequest extends GenericPageRequest {
    private String name;
}
