package com.milesight.beaveriot.device.status.service;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.*;
import com.milesight.beaveriot.context.integration.model.*;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue;
import com.milesight.beaveriot.context.model.delayedqueue.DelayedTask;
import com.milesight.beaveriot.context.support.SpringContext;
import com.milesight.beaveriot.device.status.constants.DeviceStatusConstants;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * @author Luxb
 * @date 2025/9/4 11:02
 **/
@Slf4j
@Service
public class DeviceStatusService {
    private static final int BATCH_QUERY_OFFLINE_TIMEOUT_SIZE = 1000;
    private final DeviceServiceProvider deviceServiceProvider;
    private final EntityTemplateServiceProvider entityTemplateServiceProvider;
    private final EntityServiceProvider entityServiceProvider;
    private final EntityValueServiceProvider entityValueServiceProvider;
    private final Map<String, DeviceStatusConfig> integrationDeviceStatusConfigs;
    private final DelayedQueue<Void> delayedQueue;

    public DeviceStatusService(DeviceServiceProvider deviceServiceProvider,
                               EntityTemplateServiceProvider entityTemplateServiceProvider,
                               EntityServiceProvider entityServiceProvider,
                               EntityValueServiceProvider entityValueServiceProvider,
                               DelayedQueueServiceProvider delayedQueueServiceProvider) {
        this.deviceServiceProvider = deviceServiceProvider;
        this.entityTemplateServiceProvider = entityTemplateServiceProvider;
        this.entityServiceProvider = entityServiceProvider;
        this.entityValueServiceProvider = entityValueServiceProvider;
        this.integrationDeviceStatusConfigs = new ConcurrentHashMap<>();
        this.delayedQueue = delayedQueueServiceProvider.getDelayedQueue(DeviceStatusConstants.DELAYED_QUEUE_NAME);
    }

    public void register(String integrationId, DeviceStatusConfig config) {
        if (config != null) {
            integrationDeviceStatusConfigs.put(integrationId, config);
            List<Device> devices = deviceServiceProvider.findAll(integrationId);
            if (!CollectionUtils.isEmpty(devices)) {
                initDevices(devices, config);
            }
            delayedQueue.registerConsumer(integrationId, this::consumeDelayedTask);
        }
    }

    public void deregister(Device device) {
        if (device == null) {
            return;
        }

        cancelDelayedTask(device.getId());
    }

    public void online(Device device) {
        if (device == null) {
            return;
        }

        AvailableDeviceData availableDeviceData = getAvailableDeviceDataByDevice(device);
        self().handleStatus(device.getId(), availableDeviceData, DeviceStatusOperation.ONLINE);
    }

    public void offline(Device device) {
        if (device == null) {
            return;
        }

        AvailableDeviceData availableDeviceData = getAvailableDeviceDataByDevice(device);
        self().handleStatus(device.getId(), availableDeviceData, DeviceStatusOperation.OFFLINE);
    }

    public DeviceStatus status(Device device) {
        if (device == null) {
            return null;
        }

        String deviceStatus = (String) entityValueServiceProvider.findValueByKey(getStatusEntityKey(device));
        if (deviceStatus == null) {
            return null;
        }
        return DeviceStatus.of(deviceStatus);
    }

    public Map<String, DeviceStatus> getStatusesByDeviceKeys(List<String> deviceKeys) {
        if (CollectionUtils.isEmpty(deviceKeys)) {
            return Collections.emptyMap();
        }

        Map<String, String> statusEntityKeyDeviceKeyMap = new HashMap<>();
        List<String> statusEntityKeys = new ArrayList<>();
        deviceKeys.forEach(deviceKey -> {
            String statusEntityKey = getStatusEntityKey(deviceKey);
            statusEntityKeys.add(statusEntityKey);
            statusEntityKeyDeviceKeyMap.put(statusEntityKey, deviceKey);
        });

        Map<String, Object> statusEntityValues = entityValueServiceProvider.findValuesByKeys(statusEntityKeys);
        Map<String, DeviceStatus> statuses = new HashMap<>();
        statusEntityValues.forEach((statusEntityKey, value) -> {
            if (value == null) {
                return;
            }

            String deviceKey = statusEntityKeyDeviceKeyMap.get(statusEntityKey);
            String deviceStatus = (String) value;
            statuses.put(deviceKey, DeviceStatus.of(deviceStatus));
        });
        return statuses;
    }

    @DistributedLock(name = "device:status:handle:#{#p0}", waitForLock = "5s", throwOnLockFailure = false)
    public void handleStatus(Long deviceId,
                             AvailableDeviceData availableDeviceData,
                             DeviceStatusOperation operation) {
        if (availableDeviceData.getDeviceStatusConfig() == null) {
            cancelDelayedTask(deviceId);
        }

        if (operation == DeviceStatusOperation.ONLINE) {
            handleStatusToOnline(availableDeviceData);
        } else {
            handleStatusToOffline(availableDeviceData);
        }
    }

    private void initDevices(List<Device> devices, DeviceStatusConfig config) {
        Function<List<Device>, Map<Long, Duration>> batchOfflineTimeoutFetcher = config.getBatchOfflineTimeoutFetcher();
        if (batchOfflineTimeoutFetcher == null) {
            devices.forEach(device -> {
                Duration offlineDuration = getDeviceOfflineDuration(device, config);
                if (offlineDuration != null) {
                    offerDelayedTask(device, offlineDuration);
                }
            });
        } else {
            int totalSize = devices.size();
            for (int i = 0; i < totalSize; i += BATCH_QUERY_OFFLINE_TIMEOUT_SIZE) {
                int endIndex = Math.min(i + BATCH_QUERY_OFFLINE_TIMEOUT_SIZE, totalSize);
                List<Device> batchDevices = devices.subList(i, endIndex);
                Map<Long, Duration> deviceOfflineTimeoutMap = batchOfflineTimeoutFetcher.apply(batchDevices);
                if (CollectionUtils.isEmpty(deviceOfflineTimeoutMap)) {
                    continue;
                }

                batchDevices.forEach(device -> {
                    Duration offlineDuration = deviceOfflineTimeoutMap.get(device.getId());
                    if (offlineDuration != null) {
                        offerDelayedTask(device, offlineDuration);
                    }
                });
            }
        }
    }

    private void handleStatusToOnline(AvailableDeviceData availableDeviceData) {
        Device device = availableDeviceData.getDevice();
        if (device == null) {
            return;
        }

        DeviceStatusConfig config = availableDeviceData.getDeviceStatusConfig();
        Consumer<Device> onlineListener = Optional.ofNullable(config).map(DeviceStatusConfig::getOnlineListener).orElse(null);
        updateDeviceStatusToOnline(device, onlineListener);

        Duration offlineDuration = getDeviceOfflineDuration(device, config);
        if (offlineDuration != null) {
            offerDelayedTask(device, offlineDuration);
        }
    }

    private void handleStatusToOffline(AvailableDeviceData availableDeviceData) {
        Device device = availableDeviceData.getDevice();
        if (device == null) {
            return;
        }

        DeviceStatusConfig config = availableDeviceData.getDeviceStatusConfig();
        Consumer<Device> offlineListener = Optional.ofNullable(config).map(DeviceStatusConfig::getOfflineListener).orElse(null);
        updateDeviceStatusToOffline(device, offlineListener);
    }

    private void offerDelayedTask(Device device, Duration offlineDuration) {
        delayedQueue.offer(DelayedTask.of(String.valueOf(device.getId()), device.getIntegrationId(), null, offlineDuration));
    }

    private void consumeDelayedTask(DelayedTask<Void> task) {
        Long deviceId = Long.valueOf(task.getId());
        AvailableDeviceData availableDeviceData = getAvailableDeviceDataByDeviceId(deviceId);
        self().handleStatus(deviceId, availableDeviceData, DeviceStatusOperation.OFFLINE);
    }

    private void cancelDelayedTask(Long deviceId) {
        delayedQueue.cancel(String.valueOf(deviceId));
    }

    private DeviceStatusService self() {
        return SpringContext.getBean(DeviceStatusService.class);
    }

    protected AvailableDeviceData getAvailableDeviceDataByDeviceId(Long deviceId) {
        Device device = deviceServiceProvider.findById(deviceId);
        return getAvailableDeviceDataByDevice(device);
    }

    protected AvailableDeviceData getAvailableDeviceDataByDevice(Device device) {
        if (device == null) {
            return AvailableDeviceData.of(null, null);
        }

        DeviceStatusConfig deviceStatusConfig = integrationDeviceStatusConfigs.get(device.getIntegrationId());
        return AvailableDeviceData.of(device, deviceStatusConfig);
    }

    private Duration getDeviceOfflineDuration(Device device, DeviceStatusConfig config) {
        return Optional.ofNullable(config)
                .map(DeviceStatusConfig::getOfflineTimeoutFetcher)
                .map(f -> f.apply(device))
                .filter(d -> d.toSeconds() > 0)
                .orElse(null);
    }

    private void updateDeviceStatusToOnline(Device device, Consumer<Device> onlineListener) {
        updateDeviceStatus(device, DeviceStatus.ONLINE.name(), onlineListener);
    }

    private void updateDeviceStatusToOffline(Device device, Consumer<Device> offlineListener) {
        updateDeviceStatus(device, DeviceStatus.OFFLINE.name(), offlineListener);
    }

    private void updateDeviceStatus(Device device, String deviceStatus, Consumer<Device> statusChangedListener) {
        String statusEntityKey = getStatusEntityKey(device);
        if (entityServiceProvider.findByKey(statusEntityKey) == null) {
            if (!deviceServiceProvider.existsById(device.getId())) {
                return;
            }

            EntityTemplate entityTemplate = entityTemplateServiceProvider.findByKey(DeviceStatusConstants.IDENTIFIER_DEVICE_STATUS);
            if (entityTemplate == null) {
                throw ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Device status entity template not found").build();
            }
            Entity statusEntity = entityTemplate.toEntity(device.getIntegrationId(), device.getKey());
            entityServiceProvider.save(statusEntity);
        }

        String existValue = (String) entityValueServiceProvider.findValueByKey(statusEntityKey);
        if (existValue == null && deviceStatus.equals(DeviceStatus.OFFLINE.name()) || deviceStatus.equals(existValue)) {
            return;
        }

        ExchangePayload payload = ExchangePayload.create(statusEntityKey, deviceStatus);
        entityValueServiceProvider.saveValuesAndPublishSync(payload);

        if (deviceStatus.equals(DeviceStatus.ONLINE.name())) {
            deviceOnlineCallback(device, statusChangedListener);
        } else {
            deviceOfflineCallback(device, statusChangedListener);
        }
    }

    private void deviceOnlineCallback(Device device, Consumer<Device> onlineListener) {
        log.debug("Device(id={}, key={}, name={}) status updated to online", device.getId(), device.getKey(), device.getName());
        if (onlineListener != null) {
            onlineListener.accept(device);
        }
    }

    private void deviceOfflineCallback(Device device, Consumer<Device> offlineListener) {
        log.debug("Device(id={}, key={}, name={}) status updated to offline", device.getId(), device.getKey(), device.getName());
        if (offlineListener != null) {
            offlineListener.accept(device);
        }
    }

    private String getStatusEntityKey(Device device) {
        return getStatusEntityKey(device.getKey());
    }

    private String getStatusEntityKey(String deviceKey) {
        return deviceKey + "." + DeviceStatusConstants.IDENTIFIER_DEVICE_STATUS;
    }

    @Data
    public static class AvailableDeviceData {
        private Device device;
        private DeviceStatusConfig deviceStatusConfig;

        public static AvailableDeviceData of(Device device, DeviceStatusConfig deviceStatusConfig) {
            AvailableDeviceData data = new AvailableDeviceData();
            data.setDevice(device);
            data.setDeviceStatusConfig(deviceStatusConfig);
            return data;
        }
    }

    public enum DeviceStatusOperation {
        ONLINE,
        OFFLINE
    }
}