package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;

import java.util.Map;

/**
 * @author luxb
 */
@Getter
public class DeviceTemplate implements IdentityKey {

    @Setter
    private Long id;
    private String integrationId;
    @Setter
    private String name;
    @Setter
    private String content;
    @Setter
    private String description;
    @Setter
    private Map<String, Object> additional;
    private String identifier;

    @Setter
    private Long createdAt = System.currentTimeMillis();

    @Setter
    private Long updatedAt;

    @Setter
    private String vendor;

    @Setter
    private String model;

    @Setter
    private Long blueprintLibraryId;

    @Setter
    private String blueprintLibraryVersion;

    protected DeviceTemplate() {
    }

    @Override
    public String getKey() {
        return IntegrationConstants.formatIntegrationDeviceTemplateKey(integrationId, identifier);
    }

    protected void initializeProperties(String integrationId) {
        if (integrationId == null) {
            return;
        }
        validate();
        this.setIntegrationId(integrationId);
    }

    public void validate() {
        Assert.notNull(name, "Device Template name must not be null");
        Assert.notNull(identifier, "Device Template identifier must not be null");
    }

    protected void setIntegrationId(String integrationId) {
        this.integrationId = integrationId;
    }

    protected void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
}
