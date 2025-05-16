package com.milesight.beaveriot.devicetemplate.facade;

public interface IDeviceTemplateParserFacade {
    boolean validate(String deviceTemplateContent);
    void discover(String integration, Object data, Long deviceTemplateId, String deviceTemplateContent);
}
