package com.milesight.beaveriot.context.integration.model;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.constants.IntegrationConstants;
import com.milesight.beaveriot.context.integration.enums.AccessMod;
import com.milesight.beaveriot.context.integration.enums.EntityType;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.eventbus.api.IdentityKey;
import lombok.Getter;
import lombok.Setter;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author leon
 */
@Getter
@Setter
public class Entity implements Serializable, IdentityKey {

    private Long id;
    private String deviceKey;
    private String integrationId;
    private String name;
    private String identifier;
    private AccessMod accessMod;
    private EntityValueType valueType;
    private EntityType type;
    private Map<String, Object> attributes;
    private String parentIdentifier;
    private List<Entity> children;
    private Boolean visible;
    private String description;

    protected Entity() {
    }

    public String getFullIdentifier() {
        return StringUtils.hasLength(parentIdentifier) ? parentIdentifier + "." + identifier : identifier;
    }

    @Override
    public String getKey() {
        String fullIdentifier = getFullIdentifier();
        if (StringUtils.hasText(deviceKey)) {
            return IntegrationConstants.formatIntegrationDeviceEntityKey(deviceKey, fullIdentifier);
        } else {
            return IntegrationConstants.formatIntegrationEntityKey(integrationId, fullIdentifier);
        }
    }

    public String getParentKey() {
        if (!StringUtils.hasLength(parentIdentifier)) {
            return null;
        }
        if (StringUtils.hasText(deviceKey)) {
            return IntegrationConstants.formatIntegrationDeviceEntityKey(deviceKey, parentIdentifier);
        } else {
            return IntegrationConstants.formatIntegrationEntityKey(integrationId, parentIdentifier);
        }
    }

    public void initializeProperties(String integrationId, String deviceKey) {
        validate();
        Assert.notNull(integrationId, "Integration must not be null");
        Assert.notNull(deviceKey, "Device must not be null");
        this.setIntegrationId(integrationId);
        this.setDeviceKey(deviceKey);
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(entity -> {
                entity.setDeviceKey(deviceKey);
                entity.setIntegrationId(integrationId);
                entity.setParentIdentifier(identifier);
            });
        }
    }

    protected void initializeProperties(String integrationId) {
        validate();
        Assert.notNull(integrationId, "Integration must not be null");
        this.setIntegrationId(integrationId);
        if (!CollectionUtils.isEmpty(children)) {
            children.forEach(entity -> {
                entity.setIntegrationId(integrationId);
                entity.setParentIdentifier(identifier);
            });
        }
    }

    public Optional<Device> loadDevice() {
        DeviceServiceProvider deviceServiceProvider = SpringContext.getBean(DeviceServiceProvider.class);
        return StringUtils.hasText(deviceKey) ? Optional.of(deviceServiceProvider.findByKey(deviceKey)) : Optional.empty();
    }

    public Optional<Integration> loadActiveIntegration() {
        IntegrationServiceProvider integrationServiceProvider = SpringContext.getBean(IntegrationServiceProvider.class);
        return Optional.ofNullable(integrationServiceProvider.getActiveIntegration(integrationId));
    }

    public void validate() {
        Assert.notNull(identifier, "Entity identifier must not be null");
        Assert.notNull(type, "EntityType must not be null");
        Assert.notNull(valueType, "Entity ValueType must not be null");
        Assert.notNull(name, "Entity name must not be null");
        if(type == EntityType.PROPERTY){
            Assert.notNull(accessMod, "Entity AccessMod must not be null");
        }
    }

    public void setChildren(List<Entity> children) {
        this.children = children;
        if (children != null) {
            Assert.notNull(identifier, "Entity identifier must not be null");
            children.forEach(entity -> {
                entity.setParentIdentifier(identifier);
                if (deviceKey != null) {
                    entity.setDeviceKey(deviceKey);
                }
                if (integrationId != null) {
                    entity.setIntegrationId(integrationId);
                }
            });
        }
    }

}
