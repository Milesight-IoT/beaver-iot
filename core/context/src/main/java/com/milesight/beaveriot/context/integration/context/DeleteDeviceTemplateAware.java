package com.milesight.beaveriot.context.integration.context;

import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;

/**
 * @author leon
 */
public interface DeleteDeviceTemplateAware {

    default DeviceTemplate getDeletedDeviceTemplate() {
        if (this instanceof ExchangePayload exchangePayload) {
            return (DeviceTemplate) exchangePayload.getContext(ExchangeContextKeys.DEVICE_TEMPLATE_ON_DELETE);
        } else {
            throw new UnsupportedOperationException("Class must implement ExchangePayload");
        }
    }

}
