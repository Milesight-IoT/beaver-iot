package com.milesight.beaveriot.context.integration.context;

import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;

/**
 * @author leon
 */
public interface AddDeviceAware {

    default String getAddDeviceName() {
        if (this instanceof ExchangePayload exchangePayload) {
            return (String) exchangePayload.getContext(ExchangeContextKeys.DEVICE_NAME_ON_ADD);
        } else {
            throw new UnsupportedOperationException("Class must implement ExchangePayload");
        }
    }

    default String getAddDeviceTemplateKey() {
        if (this instanceof ExchangePayload exchangePayload) {
            return (String) exchangePayload.getContext(ExchangeContextKeys.DEVICE_TEMPLATE_KEY_ON_ADD);
        } else {
            throw new UnsupportedOperationException("Class must implement ExchangePayload");
        }
    }
}
