package com.milesight.beaveriot.rule.manager.model.request;

import com.milesight.beaveriot.base.page.GenericPageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class SearchWorkflowRequest extends GenericPageRequest {
    private String name;

    private Boolean enabled;
}
