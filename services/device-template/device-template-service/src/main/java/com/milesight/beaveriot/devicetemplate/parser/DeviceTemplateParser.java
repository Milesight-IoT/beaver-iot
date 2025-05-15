package com.milesight.beaveriot.devicetemplate.parser;

import com.milesight.beaveriot.devicetemplate.facade.IDeviceTemplateParserFacade;

/**
 * author: Luxb
 * create: 2025/5/15 13:22
 **/
abstract public class DeviceTemplateParser implements IDeviceTemplateParserFacade {
    abstract public boolean validate(String deviceTemplateContent);
}
