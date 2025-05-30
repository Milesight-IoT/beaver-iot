package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.context.support.IdentifierValidator;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * @author luxb
 */
public class BaseDeviceTemplateBuilder<T extends BaseDeviceTemplateBuilder> {

    protected List<Entity> entities;
    protected String name;
    protected String content;
    protected String description;
    protected String identifier;
    protected Map<String, Object> additional;
    protected String integrationId;
    protected Long id;

    public BaseDeviceTemplateBuilder(String integrationId) {
        this.integrationId = integrationId;
    }

    public BaseDeviceTemplateBuilder() {
    }

    public T id(Long id) {
        this.id = id;
        return (T) this;
    }

    public T name(String name) {
        this.name = name;
        return (T) this;
    }

    public T content(String content) {
        this.content = content;
        return (T) this;
    }

    public T description(String description) {
        this.description = description;
        return (T) this;
    }

    public T identifier(String identifier) {
        IdentifierValidator.validate(identifier);
        this.identifier = identifier;
        return (T) this;
    }

    public T additional(Map<String, Object> additional) {
        this.additional = additional;
        return (T) this;
    }

    public DeviceTemplate build() {
        DeviceTemplate deviceTemplate = new DeviceTemplate();
        deviceTemplate.setName(name);
        deviceTemplate.setContent(content);
        deviceTemplate.setDescription(description);
        deviceTemplate.setAdditional(additional);
        deviceTemplate.setIdentifier(identifier);
        if (StringUtils.hasText(integrationId)) {
            deviceTemplate.setIntegrationId(integrationId);
            deviceTemplate.initializeProperties(integrationId);
        }
        deviceTemplate.setId(id);
        return deviceTemplate;
    }

}
