package com.milesight.beaveriot.device.location.service;

import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
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

    public DeviceLocation getDeviceLocation(String deviceKey) {
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

        String address = (String) values.getOrDefault(addressKey, "");
        Double longitude = (Double) values.get(longitudeKey);
        Double latitude = (Double) values.get(latitudeKey);
        return DeviceLocation.of(address, longitude, latitude);
    }

    public void setDeviceLocation(String deviceKey, DeviceLocation deviceLocation) {
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
        entityValueServiceProvider.saveValues(exchangePayload);
    }
}
