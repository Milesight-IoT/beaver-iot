package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.context.support.IdentifierValidator;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author luxb
 */
public class BaseDeviceTemplateBuilder<T extends BaseDeviceTemplateBuilder> {
    protected String name;
    protected String content = "";
    protected String description = "";
    protected String identifier;
    protected Map<String, Object> additional;
    protected String integrationId;
    protected Long id;
    protected String vendor;
    protected String model;
    protected Long blueprintLibraryId;
    protected String blueprintLibraryVersion;

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

    public T vendor(String vendor) {
        this.vendor = vendor;
        return (T) this;
    }

    public T model(String model) {
        this.model = model;
        return (T) this;
    }

    public T blueprintLibraryId(Long blueprintLibraryId) {
        this.blueprintLibraryId = blueprintLibraryId;
        return (T) this;
    }

    public T blueprintLibraryVersion(String blueprintLibraryVersion) {
        this.blueprintLibraryVersion = blueprintLibraryVersion;
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
        deviceTemplate.setVendor(vendor);
        deviceTemplate.setModel(model);
        deviceTemplate.setBlueprintLibraryId(blueprintLibraryId);
        deviceTemplate.setBlueprintLibraryVersion(blueprintLibraryVersion);
        return deviceTemplate;
    }

}
