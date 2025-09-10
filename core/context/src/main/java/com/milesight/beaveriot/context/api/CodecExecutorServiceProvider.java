package com.milesight.beaveriot.context.api;

/**
 * author: Luxb
 * create: 2025/9/8 17:52
 **/
public interface CodecExecutorServiceProvider {
    DeviceCodecExecutorProvider getDeviceCodecExecutor(String vendor, String model);
}