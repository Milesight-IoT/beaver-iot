package com.milesight.beaveriot.context.integration.context;

import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;

/**
 * @author leon
 */
public interface DeleteDeviceAware {

    default Device getDeletedDevice() {
        if (this instanceof ExchangePayload exchangePayload) {
            return (Device) exchangePayload.getContext(ExchangeContextKeys.DEVICE_ON_DELETE);
        } else {
            throw new UnsupportedOperationException("Class must implement ExchangePayload");
        }
    }

}
