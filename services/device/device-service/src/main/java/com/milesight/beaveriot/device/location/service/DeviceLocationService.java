package com.milesight.beaveriot.device.location.service;

import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityTemplateServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.device.location.constants.DeviceLocationConstants;
import com.milesight.beaveriot.device.location.support.DeviceLocationSupport;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * author: Luxb
 * create: 2025/10/13 11:10
 **/
@Service
public class DeviceLocationService {
    private final EntityTemplateServiceProvider entityTemplateServiceProvider;
    private final EntityServiceProvider entityServiceProvider;
    private final EntityValueServiceProvider entityValueServiceProvider;

    public DeviceLocationService(EntityTemplateServiceProvider entityTemplateServiceProvider, @Lazy EntityServiceProvider entityServiceProvider, EntityValueServiceProvider entityValueServiceProvider) {
        this.entityTemplateServiceProvider = entityTemplateServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
        this.entityValueServiceProvider = entityValueServiceProvider;
    }

    public DeviceLocation getLocation(Device device) {
        String deviceKey = device.getKey();
        List<String> locationKeys = new ArrayList<>();

        String latitudeKey = DeviceLocationSupport.getLatitudeEntityKey(deviceKey);
        String longitudeKey = DeviceLocationSupport.getLongitudeEntityKey(deviceKey);
        String addressKey = DeviceLocationSupport.getAddressEntityKey(deviceKey);
        locationKeys.add(latitudeKey);
        locationKeys.add(longitudeKey);
        locationKeys.add(addressKey);

        Map<String, Object> values = entityValueServiceProvider.findValuesByKeys(locationKeys);
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }

        Double latitude = (Double) values.get(latitudeKey);
        Double longitude = (Double) values.get(longitudeKey);
        String address = (String) values.get(addressKey);
        return DeviceLocation.of(latitude, longitude, address);
    }

    public void setLocation(Device device, DeviceLocation location) {
        if (location == null) {
            return;
        }

        location.formatAddress();
        DeviceLocationSupport.validate(location);

        createLocationEntityIfNotExist(device);

        String deviceKey = device.getKey();
        String latitudeKey = DeviceLocationSupport.getLatitudeEntityKey(deviceKey);
        String longitudeKey = DeviceLocationSupport.getLongitudeEntityKey(deviceKey);
        String addressKey = DeviceLocationSupport.getAddressEntityKey(deviceKey);

        ExchangePayload exchangePayload = new ExchangePayload();
        exchangePayload.put(latitudeKey, location.getLatitude());
        exchangePayload.put(longitudeKey, location.getLongitude());
        exchangePayload.put(addressKey, location.getAddress());

        exchangePayload.validate();
        entityValueServiceProvider.saveValuesAndPublishSync(exchangePayload);
    }

    public void clearLocation(Device device) {
        String deviceKey = device.getKey();
        String latitudeKey = DeviceLocationSupport.getLatitudeEntityKey(deviceKey);
        String longitudeKey = DeviceLocationSupport.getLongitudeEntityKey(deviceKey);
        String addressKey = DeviceLocationSupport.getAddressEntityKey(deviceKey);

        ExchangePayload exchangePayload = new ExchangePayload();
        exchangePayload.put(latitudeKey, null);
        exchangePayload.put(longitudeKey, null);
        exchangePayload.put(addressKey, null);

        entityValueServiceProvider.saveValues(exchangePayload);
    }

    public Map<String, DeviceLocation> getLocationsByDeviceKeys(List<String> deviceKeys) {
        if (CollectionUtils.isEmpty(deviceKeys)) {
            return Collections.emptyMap();
        }

        Map<String, String> latitudeEntityKeyDeviceKeyMap = new HashMap<>();
        Map<String, String> longitudeEntityKeyDeviceKeyMap = new HashMap<>();
        Map<String, String> addressEntityKeyDeviceKeyMap = new HashMap<>();
        List<String> latitudeEntityKeys = new ArrayList<>();
        List<String> longitudeEntityKeys = new ArrayList<>();
        List<String> addressEntityKeys = new ArrayList<>();
        deviceKeys.forEach(deviceKey -> {
            String latitudeEntityKey = DeviceLocationSupport.getLatitudeEntityKey(deviceKey);
            latitudeEntityKeys.add(latitudeEntityKey);
            latitudeEntityKeyDeviceKeyMap.put(latitudeEntityKey, deviceKey);

            String longitudeEntityKey = DeviceLocationSupport.getLongitudeEntityKey(deviceKey);
            longitudeEntityKeys.add(longitudeEntityKey);
            longitudeEntityKeyDeviceKeyMap.put(longitudeEntityKey, deviceKey);

            String addressEntityKey = DeviceLocationSupport.getAddressEntityKey(deviceKey);
            addressEntityKeys.add(addressEntityKey);
            addressEntityKeyDeviceKeyMap.put(addressEntityKey, deviceKey);
        });

        Map<String, DeviceLocation> locations = new HashMap<>();
        Map<String, Object> latitudeEntityValues = entityValueServiceProvider.findValuesByKeys(latitudeEntityKeys);
        latitudeEntityValues.forEach((latitudeEntityKey, value) -> {
            if (value == null) {
                return;
            }

            String deviceKey = latitudeEntityKeyDeviceKeyMap.get(latitudeEntityKey);
            Double latitude = (Double) value;
            DeviceLocation location = locations.computeIfAbsent(deviceKey, key -> new DeviceLocation());
            location.setLatitude(latitude);
        });

        Map<String, Object> longitudeEntityValues = entityValueServiceProvider.findValuesByKeys(longitudeEntityKeys);
        longitudeEntityValues.forEach((longitudeEntityKey, value) -> {
            if (value == null) {
                return;
            }

            String deviceKey = longitudeEntityKeyDeviceKeyMap.get(longitudeEntityKey);
            Double longitude = (Double) value;
            DeviceLocation location = locations.computeIfAbsent(deviceKey, key -> new DeviceLocation());
            location.setLongitude(longitude);
        });

        Map<String, Object> addressEntityValues = entityValueServiceProvider.findValuesByKeys(addressEntityKeys);
        addressEntityValues.forEach((addressEntityKey, value) -> {
            if (value == null) {
                return;
            }

            String deviceKey = addressEntityKeyDeviceKeyMap.get(addressEntityKey);
            String address = (String) value;
            DeviceLocation location = locations.computeIfAbsent(deviceKey, key -> new DeviceLocation());
            location.setAddress(address);
        });

        return locations;
    }

    private void createLocationEntityIfNotExist(Device device) {
        String locationEntityKey = DeviceLocationSupport.getLocationEntityKey(device.getKey());
        if (entityServiceProvider.findByKey(locationEntityKey) == null) {
            EntityTemplate entityTemplate = entityTemplateServiceProvider.findByKey(DeviceLocationConstants.IDENTIFIER_DEVICE_LOCATION);
            if (entityTemplate == null) {
                throw new RuntimeException("Device location entity template not found");
            }
            Entity locationEntity = entityTemplate.toEntity(device.getIntegrationId(), device.getKey());
            entityServiceProvider.save(locationEntity);
        }
    }
}
