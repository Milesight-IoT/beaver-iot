package com.milesight.beaveriot.device.status.redis;

import com.milesight.beaveriot.context.api.DeviceServiceProvider;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.device.status.AbstractDeviceStatusManager;
import com.milesight.beaveriot.device.status.DeviceStatusManager;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * author: Luxb
 * create: 2025/9/4 8:57
 **/
@Slf4j
public class DeviceStatusRedisManager extends AbstractDeviceStatusManager implements DeviceStatusManager {
    private static final String DEVICE_STATUS_TIMEOUT_QUEUE = "device:status:timeout:queue";
    private static final String DEVICE_STATUS_TIMEOUT_MAP = "device:status:timeout:map";
    private final RedissonClient redissonClient;
    private RBlockingQueue<String> deviceTimeoutQueue;
    private RDelayedQueue<String> delayedQueue;
    private RMap<String, Long> deviceExpirationTimeMap;
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

    private void listenDeviceStatusDelayedQueue() {
        while(isRunning.get()) {
            try {
                String deviceKey = deviceTimeoutQueue.take();
                Long expirationTime = deviceExpirationTimeMap.get(deviceKey);
                if (expirationTime == null) {
                    doUpdateDeviceStatusToOffline(deviceKey);
                    continue;
                }

                long currentTime = System.currentTimeMillis();
                if (currentTime < expirationTime) {
                    continue;
                }

                doUpdateDeviceStatusToOffline(deviceKey);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("Error occurred while listening device status delayed queue", e);
            }
        }
    }

    private void doUpdateDeviceStatusToOffline(String deviceKey) {
        deviceExpirationTimeMap.remove(deviceKey);

        updateDeviceStatusToOfflineByDeviceKey(deviceKey);
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

        String tenantId = TenantContext.getTenantId();
        executorService.execute(() -> {
            TenantContext.setTenantId(tenantId);
            listenDeviceStatusDelayedQueue();
        });
    }

    @Override
    public void dataUploaded(Device device, ExchangePayload payload) {
        DeviceStatusConfig deviceStatusConfig = integrationDeviceStatusConfigs.get(device.getIntegrationId());
        if (deviceStatusConfig == null) {
            return;
        }

        deviceStatusConfig.getOnlineUpdater().accept(device, payload);
        deviceExpirationTimeMap.put(device.getKey(), System.currentTimeMillis() + deviceStatusConfig.getOfflineSecondsFetcher().apply(device) * 1000);
        delayedQueue.offer(device.getKey(), deviceStatusConfig.getOfflineSecondsFetcher().apply(device), TimeUnit.SECONDS);
    }

    @Override
    public void deviceDeleted(Device device) {
        deviceExpirationTimeMap.remove(device.getKey());
    }
}