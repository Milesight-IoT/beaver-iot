package com.milesight.beaveriot.device.status.redis;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.annotations.shedlock.LockScope;
import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.device.status.BaseDeviceStatusManager;
import com.milesight.beaveriot.device.status.DeviceStatusManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.aop.framework.AopContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * author: Luxb
 * create: 2025/9/4 8:57
 **/
@Slf4j
public class DeviceStatusRedisManager extends BaseDeviceStatusManager implements DeviceStatusManager {
    private static final String DEVICE_STATUS_TIMEOUT_QUEUE = "device:status:timeout:queue";
    private static final String DEVICE_STATUS_TIMEOUT_MAP = "device:status:timeout:map";
    private final RedissonClient redissonClient;
    private RBlockingQueue<Long> deviceTimeoutQueue;
    private RDelayedQueue<Long> delayedQueue;
    private RMap<Long, Long> deviceExpirationTimeMap;
    private final ExecutorService executorService;
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicBoolean isListened = new AtomicBoolean(false);

    public DeviceStatusRedisManager(DeviceServiceProvider deviceServiceProvider,
                                    EntityServiceProvider entityServiceProvider,
                                    EntityValueServiceProvider entityValueServiceProvider,
                                    RedissonClient redissonClient) {
        super(deviceServiceProvider, entityServiceProvider, entityValueServiceProvider);
        this.redissonClient = redissonClient;
        this.executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void init() {
        deviceTimeoutQueue = redissonClient.getBlockingQueue(DEVICE_STATUS_TIMEOUT_QUEUE);
        delayedQueue = redissonClient.getDelayedQueue(deviceTimeoutQueue);
        deviceExpirationTimeMap = redissonClient.getMap(DEVICE_STATUS_TIMEOUT_MAP);
    }

    @Override
    protected void destroy() {
        isRunning.set(false);
        if (!executorService.isShutdown()) {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    protected void afterRegister(String integrationId, DeviceStatusConfig config) {
        if (!isListened.compareAndSet(false, true)) {
            return;
        }

        DeviceStatusRedisManager self = self();
        String tenantId = TenantContext.getTenantId();
        executorService.execute(() -> {
            TenantContext.setTenantId(tenantId);
            listenDeviceStatusDelayedQueue(self);
        });
    }

    private void listenDeviceStatusDelayedQueue(DeviceStatusRedisManager self) {
        while(isRunning.get()) {
            try {
                Long deviceId = deviceTimeoutQueue.take();
                AvailableDeviceData availableDeviceData = getAvailableDeviceDataByDeviceId(deviceId);
                self.handleStatus(deviceId, availableDeviceData, null, DeviceStatusOperation.OFFLINE);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error occurred while listening device status delayed queue", e);
            }
        }
    }

    @Override
    public void dataUploaded(Device device, ExchangePayload payload) {
        AvailableDeviceData availableDeviceData = getAvailableDeviceDataByDeviceId(device.getId());
        self().handleStatus(device.getId(), availableDeviceData, payload, DeviceStatusOperation.ONLINE);
    }

    @Override
    public void deviceDeleted(Device device) {
        deviceExpirationTimeMap.remove(device.getId());
    }

    private DeviceStatusRedisManager self() {
        return (DeviceStatusRedisManager) AopContext.currentProxy();
    }

    @Transactional
    @DistributedLock(name = "device:status:handle:#{#p0}", waitForLock = "5s", throwOnLockFailure = false, scope = LockScope.GLOBAL)
    public void handleStatus(Long deviceId,
                             AvailableDeviceData availableDeviceData,
                             ExchangePayload payload,
                             DeviceStatusOperation operation) {
        if (availableDeviceData == null) {
            deviceExpirationTimeMap.remove(deviceId);
            return;
        }

        if (operation == DeviceStatusOperation.ONLINE) {
            handleStatusToOnline(availableDeviceData, payload);
        } else {
            handleStatusToOffline(availableDeviceData);
        }
    }

    private void handleStatusToOnline(AvailableDeviceData availableDeviceData, ExchangePayload payload) {
        Device device = availableDeviceData.getDevice();
        DeviceStatusConfig config = availableDeviceData.getDeviceStatusConfig();
        config.getOnlineUpdater().accept(device, payload);

        long offlineSeconds = getDeviceOfflineSeconds(device, config);
        long expirationTime = System.currentTimeMillis() + offlineSeconds * 1000;
        deviceExpirationTimeMap.put(device.getId(), expirationTime);
        delayedQueue.offer(device.getId(), offlineSeconds, TimeUnit.SECONDS);
        deviceOnlineCallback(device, expirationTime / 1000);
    }

    private void handleStatusToOffline(AvailableDeviceData availableDeviceData) {
        Device device = availableDeviceData.getDevice();
        Long expirationTime = deviceExpirationTimeMap.get(device.getId());
        if (expirationTime == null) {
            doUpdateDeviceStatusToOffline(availableDeviceData);
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime < expirationTime) {
            return;
        }

        doUpdateDeviceStatusToOffline(availableDeviceData);
    }

    private void doUpdateDeviceStatusToOffline(AvailableDeviceData availableDeviceData) {
        deviceExpirationTimeMap.remove(availableDeviceData.getDevice().getId());

        availableDeviceData.getDeviceStatusConfig().getOfflineUpdater().accept(availableDeviceData.getDevice());
        deviceOfflineCallback(availableDeviceData.getDevice());
    }

    public enum DeviceStatusOperation {
        ONLINE,
        OFFLINE
    }
}