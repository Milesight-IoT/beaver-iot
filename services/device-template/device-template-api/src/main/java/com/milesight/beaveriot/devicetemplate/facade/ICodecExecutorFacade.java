package com.milesight.beaveriot.devicetemplate.facade;

import com.milesight.beaveriot.context.model.BlueprintLibrary;

/**
 * author: Luxb
 * create: 2025/9/16 9:00
 **/
public interface ICodecExecutorFacade {
    IDeviceCodecExecutorFacade getDeviceCodecExecutor(BlueprintLibrary blueprintLibrary, String vendor, String model);
}
