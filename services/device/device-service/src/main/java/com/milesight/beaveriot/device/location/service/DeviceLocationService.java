package com.milesight.beaveriot.device.location.service;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.device.location.model.DeviceLocation;
import com.milesight.beaveriot.device.location.support.DeviceLocationSupport;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * author: Luxb
 * create: 2025/10/13 11:10
 **/
@Service
public class DeviceLocationService {
    private final EntityValueServiceProvider entityValueServiceProvider;

    public DeviceLocationService(EntityValueServiceProvider entityValueServiceProvider) {
        this.entityValueServiceProvider = entityValueServiceProvider;
    }

    public DeviceLocation getLocation(Device device) {
        String deviceKey = device.getKey();
        List<String> locationKeys = new ArrayList<>();
        String addressKey = DeviceLocationSupport.getAddressKey(deviceKey);
        String longitudeKey = DeviceLocationSupport.getLongitudeKey(deviceKey);
        String latitudeKey = DeviceLocationSupport.getLatitudeKey(deviceKey);
        locationKeys.add(addressKey);
        locationKeys.add(longitudeKey);
        locationKeys.add(latitudeKey);

        Map<String, Object> values = entityValueServiceProvider.findValuesByKeys(locationKeys);
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }

        String address = (String) values.get(addressKey);
        Double longitude = (Double) values.get(longitudeKey);
        Double latitude = (Double) values.get(latitudeKey);
        return DeviceLocation.of(address, longitude, latitude);
    }

    public void setLocation(Device device, DeviceLocation deviceLocation) {
        String deviceKey = device.getKey();
        String addressKey = DeviceLocationSupport.getAddressKey(deviceKey);
        String longitudeKey = DeviceLocationSupport.getLongitudeKey(deviceKey);
        String latitudeKey = DeviceLocationSupport.getLatitudeKey(deviceKey);

        ExchangePayload exchangePayload = new ExchangePayload();
        if (deviceLocation.getAddress() != null) {
            exchangePayload.put(addressKey, deviceLocation.getAddress());
        }
        if (deviceLocation.getLongitude() != null) {
            exchangePayload.put(longitudeKey, deviceLocation.getLongitude());
        }
        if (deviceLocation.getLatitude() != null) {
            exchangePayload.put(latitudeKey, deviceLocation.getLatitude());
        }

        if (exchangePayload.isEmpty()) {
            return;
        }

        exchangePayload.validate();
        entityValueServiceProvider.saveValues(exchangePayload);
    }

    public void clearLocation(Device device) {
        String deviceKey = device.getKey();
        String addressKey = DeviceLocationSupport.getAddressKey(deviceKey);
        String longitudeKey = DeviceLocationSupport.getLongitudeKey(deviceKey);
        String latitudeKey = DeviceLocationSupport.getLatitudeKey(deviceKey);

        ExchangePayload exchangePayload = new ExchangePayload();
        exchangePayload.put(addressKey, null);
        exchangePayload.put(longitudeKey, null);
        exchangePayload.put(latitudeKey, null);

        entityValueServiceProvider.saveValues(exchangePayload);
    }
}
