package com.milesight.beaveriot.context.integration.context;

import com.milesight.beaveriot.context.constants.ExchangeContextKeys;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;

/**
 * @author luxb
 */
public interface UpdateDeviceTemplateAware {

    default String getUpdateDeviceTemplateName() {
        if (this instanceof ExchangePayload exchangePayload) {
            return (String) exchangePayload.getContext(ExchangeContextKeys.DEVICE_TEMPLATE_NAME_ON_UPDATE);
        } else {
            throw new UnsupportedOperationException("Class must implement ExchangePayload");
        }
    }

    default String getUpdateDeviceTemplateContent() {
        if (this instanceof ExchangePayload exchangePayload) {
            return (String) exchangePayload.getContext(ExchangeContextKeys.DEVICE_TEMPLATE_CONTENT_ON_UPDATE);
        } else {
            throw new UnsupportedOperationException("Class must implement ExchangePayload");
        }
    }

    default String getUpdateDeviceTemplateDescription() {
        if (this instanceof ExchangePayload exchangePayload) {
            return (String) exchangePayload.getContext(ExchangeContextKeys.DEVICE_TEMPLATE_DESCRIPTION_ON_UPDATE);
        } else {
            throw new UnsupportedOperationException("Class must implement ExchangePayload");
        }
    }

    default Long getUpdateDeviceTemplateId() {
        if (this instanceof ExchangePayload exchangePayload) {
            return (Long) exchangePayload.getContext(ExchangeContextKeys.DEVICE_TEMPLATE_ID_ON_UPDATE);
        } else {
            throw new UnsupportedOperationException("Class must implement ExchangePayload");
        }
    }
}
