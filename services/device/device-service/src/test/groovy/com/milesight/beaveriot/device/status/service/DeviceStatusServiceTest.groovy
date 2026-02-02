package com.milesight.beaveriot.device.status.service

import com.milesight.beaveriot.context.api.*
import com.milesight.beaveriot.context.integration.model.*
import com.milesight.beaveriot.context.model.delayedqueue.DelayedQueue
import com.milesight.beaveriot.context.support.SpringContext
import com.milesight.beaveriot.device.status.constants.DeviceStatusConstants
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Field
import java.time.Duration

/**
 * @author Luxb
 * @date 2025/11/19 13:11
 * */
class DeviceStatusServiceTest extends Specification {

    DeviceServiceProvider deviceServiceProvider
    EntityTemplateServiceProvider entityTemplateServiceProvider
    EntityServiceProvider entityServiceProvider
    EntityValueServiceProvider entityValueServiceProvider
    DelayedQueueServiceProvider delayedQueueServiceProvider
    DelayedQueue<Void> delayedQueue
    DeviceStatusService deviceStatusService

    def setup() {
        deviceServiceProvider = Mock(DeviceServiceProvider)
        entityTemplateServiceProvider = Mock(EntityTemplateServiceProvider)
        entityServiceProvider = Mock(EntityServiceProvider)
        entityValueServiceProvider = Mock(EntityValueServiceProvider)
        delayedQueueServiceProvider = Mock(DelayedQueueServiceProvider)
        delayedQueue = Mock(DelayedQueue)
        delayedQueueServiceProvider.getDelayedQueue(DeviceStatusConstants.DELAYED_QUEUE_NAME) >> delayedQueue

        deviceStatusService = new DeviceStatusService(
                deviceServiceProvider,
                entityTemplateServiceProvider,
                entityServiceProvider,
                entityValueServiceProvider,
                delayedQueueServiceProvider
        )

        ConfigurableListableBeanFactory mockBeanFactory = Mock(ConfigurableListableBeanFactory)
        mockBeanFactory.getBean(DeviceStatusService.class) >> deviceStatusService

        Field beanFactoryField = SpringContext.class.getDeclaredField("beanFactory")
        beanFactoryField.setAccessible(true)
        beanFactoryField.set(null, mockBeanFactory)
    }

    // ==================== register tests ====================

    def "register should register consumer and init devices when config is provided"() {
        given:
        def integrationId = "test-integration"
        def device = createDevice(1L, integrationId, "device-1")
        def config = DeviceStatusConfig.builder()
                .offlineTimeoutFetcher({ d -> Duration.ofMinutes(5) })
                .build()
        deviceServiceProvider.findAll(integrationId) >> [device]

        when:
        deviceStatusService.register(integrationId, config)

        then:
        1 * delayedQueue.registerConsumer(integrationId, _)
        1 * delayedQueue.offer(_)
    }

    def "register should do nothing when config is null"() {
        given:
        def integrationId = "test-integration"

        when:
        deviceStatusService.register(integrationId, null)

        then:
        0 * deviceServiceProvider.findAll(_)
        0 * delayedQueue.registerConsumer(_, _)
        0 * delayedQueue.offer(_)
    }

    def "register should only register consumer when devices list is empty"() {
        given:
        def integrationId = "test-integration"
        def config = DeviceStatusConfig.builder()
                .offlineTimeoutFetcher({ d -> Duration.ofMinutes(5) })
                .build()
        deviceServiceProvider.findAll(integrationId) >> []

        when:
        deviceStatusService.register(integrationId, config)

        then:
        1 * delayedQueue.registerConsumer(integrationId, _)
        0 * delayedQueue.offer(_)
    }

    def "register should use batchOfflineTimeoutFetcher when provided"() {
        given:
        def integrationId = "test-integration"
        def device1 = createDevice(1L, integrationId, "device-1")
        def device2 = createDevice(2L, integrationId, "device-2")
        def config = DeviceStatusConfig.builder()
                .batchOfflineTimeoutFetcher({ devices ->
                    [(1L): Duration.ofMinutes(5), (2L): Duration.ofMinutes(10)]
                })
                .build()
        deviceServiceProvider.findAll(integrationId) >> [device1, device2]

        when:
        deviceStatusService.register(integrationId, config)

        then:
        1 * delayedQueue.registerConsumer(integrationId, _)
        2 * delayedQueue.offer(_)
    }

    // ==================== deregister tests ====================

    def "deregister should cancel delayed task for device"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")

        when:
        deviceStatusService.deregister(device)

        then:
        1 * delayedQueue.cancel("1")
    }

    // ==================== online tests ====================

    def "online should update device status when no config registered"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def entityTemplate = Mock(EntityTemplate)
        def entity = Mock(Entity)

        deviceServiceProvider.existsById(1L) >> true
        entityServiceProvider.findByKey(statusEntityKey) >> null
        entityTemplateServiceProvider.findByKey(DeviceStatusConstants.IDENTIFIER_DEVICE_STATUS) >> entityTemplate
        entityTemplate.toEntity("test-integration", device.getKey()) >> entity
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> null

        when:
        deviceStatusService.online(device)

        then:
        1 * entityServiceProvider.save(entity)
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    // ==================== offline tests ====================

    def "offline should update device status to offline"() {
        given:
        def integrationId = "test-integration"
        def device = createDevice(1L, integrationId, "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def entityTemplate = Mock(EntityTemplate)
        def entity = Mock(Entity)
        def offlineListenerCalled = false
        def config = DeviceStatusConfig.builder()
                .offlineListener({ d -> offlineListenerCalled = true })
                .build()

        // Register the integration first
        deviceServiceProvider.findAll(integrationId) >> []
        deviceStatusService.register(integrationId, config)

        deviceServiceProvider.existsById(1L) >> true
        entityServiceProvider.findByKey(statusEntityKey) >> null
        entityTemplateServiceProvider.findByKey(DeviceStatusConstants.IDENTIFIER_DEVICE_STATUS) >> entityTemplate
        entityTemplate.toEntity(integrationId, device.getKey()) >> entity
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "ONLINE"

        when:
        deviceStatusService.offline(device)

        then:
        1 * entityServiceProvider.save(entity)
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
        offlineListenerCalled
    }

    def "offline should update status even without registered config"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def entityTemplate = Mock(EntityTemplate)
        def entity = Mock(Entity)

        deviceServiceProvider.existsById(1L) >> true
        entityServiceProvider.findByKey(statusEntityKey) >> null
        entityTemplateServiceProvider.findByKey(DeviceStatusConstants.IDENTIFIER_DEVICE_STATUS) >> entityTemplate
        entityTemplate.toEntity("test-integration", device.getKey()) >> entity
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "ONLINE"

        when:
        deviceStatusService.offline(device)

        then:
        1 * entityServiceProvider.save(entity)
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    // ==================== offlineIfPreviouslySeen tests ====================

    def "offlineIfPreviouslySeen should do nothing when device is null"() {
        when:
        deviceStatusService.offlineIfPreviouslySeen(null)

        then:
        0 * deviceServiceProvider._
        0 * entityServiceProvider._
        0 * entityValueServiceProvider._
    }

    def "offlineIfPreviouslySeen should update to offline when device was previously online"() {
        given:
        def integrationId = "test-integration"
        def device = createDevice(1L, integrationId, "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def config = DeviceStatusConfig.builder()
                .offlineListener({ d -> })
                .build()

        // Register the integration first
        deviceServiceProvider.findAll(integrationId) >> []
        deviceStatusService.register(integrationId, config)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "ONLINE"

        when:
        deviceStatusService.offlineIfPreviouslySeen(device)

        then:
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    def "offlineIfPreviouslySeen should not update when device has no previous status"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> null

        when:
        deviceStatusService.offlineIfPreviouslySeen(device)

        then:
        0 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    def "offlineIfPreviouslySeen should call offline listener when status changes"() {
        given:
        def integrationId = "test-integration"
        def device = createDevice(1L, integrationId, "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def offlineListenerCalled = false
        def config = DeviceStatusConfig.builder()
                .offlineListener({ d -> offlineListenerCalled = true })
                .build()

        // Register the integration first
        deviceServiceProvider.findAll(integrationId) >> []
        deviceStatusService.register(integrationId, config)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "ONLINE"

        when:
        deviceStatusService.offlineIfPreviouslySeen(device)

        then:
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
        offlineListenerCalled
    }

    def "offlineIfPreviouslySeen should work without registered config"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "ONLINE"

        when:
        deviceStatusService.offlineIfPreviouslySeen(device)

        then:
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    def "offlineIfPreviouslySeen should not update when device already offline"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "OFFLINE"

        when:
        deviceStatusService.offlineIfPreviouslySeen(device)

        then:
        0 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    // ==================== status tests ====================

    @Unroll
    def "status returns #expected for device with status value '#statusValue'"() {
        given:
        def device = createDevice(1L, "test-integration", deviceIdentifier)
        def statusEntityKey = getDeviceStatusEntityKey(device)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> statusValue

        expect:
        deviceStatusService.status(device) == expected

        where:
        deviceIdentifier  | statusValue | expected
        "device-1" | "ONLINE"    | DeviceStatus.ONLINE
        "device-2" | "OFFLINE"   | DeviceStatus.OFFLINE
        "device-3" | "online"    | DeviceStatus.ONLINE
        "device-4" | "offline"   | DeviceStatus.OFFLINE
        "device-5" | null        | null
    }

    // ==================== getStatusesByDeviceKeys tests ====================

    def "getStatusesByDeviceKeys returns empty map when deviceKeys is empty"() {
        expect:
        deviceStatusService.getStatusesByDeviceKeys([]) == [:]
    }

    def "getStatusesByDeviceKeys returns empty map when deviceKeys is null"() {
        expect:
        deviceStatusService.getStatusesByDeviceKeys(null) == [:]
    }

    def "getStatusesByDeviceKeys returns statuses for multiple devices"() {
        given:
        def integrationId = "test-integration"
        def deviceIdentifiers = ["device-1", "device-2", "device-3"]
        def deviceKeys = deviceIdentifiers.collect { integrationId + ".device." + it }
        def statusEntityKeys = deviceKeys.collect { it + ".@status" }
        entityValueServiceProvider.findValuesByKeys(statusEntityKeys) >> [
                "test-integration.device.device-1.@status": "ONLINE",
                "test-integration.device.device-2.@status": "OFFLINE",
                "test-integration.device.device-3.@status": null
        ]

        when:
        def result = deviceStatusService.getStatusesByDeviceKeys(deviceKeys)

        then:
        result.size() == 2
        result["test-integration.device.device-1"] == DeviceStatus.ONLINE
        result["test-integration.device.device-2"] == DeviceStatus.OFFLINE
        !result.containsKey("test-integration.device.device-3")
    }

    // ==================== handleStatus tests ====================

    def "handleStatus should cancel delayed task when deviceStatusConfig is null"() {
        given:
        def deviceId = 1L
        def device = createDevice(1L, "test-integration", "device-1")
        def availableDeviceData = DeviceStatusService.AvailableDeviceData.of(device, null)
        def statusEntityKey = getDeviceStatusEntityKey(device)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "OFFLINE"

        when:
        deviceStatusService.handleStatus(deviceId, availableDeviceData, DeviceStatusService.DeviceStatusOperation.ONLINE, true)

        then:
        1 * delayedQueue.cancel("1")
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    def "handleStatus should handle online operation"() {
        given:
        def integrationId = "test-integration"
        def device = createDevice(1L, integrationId, "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def config = DeviceStatusConfig.builder()
                .offlineTimeoutFetcher({ d -> Duration.ofMinutes(5) })
                .build()
        def availableDeviceData = DeviceStatusService.AvailableDeviceData.of(device, config)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "OFFLINE"

        when:
        deviceStatusService.handleStatus(1L, availableDeviceData, DeviceStatusService.DeviceStatusOperation.ONLINE, true)

        then:
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
        1 * delayedQueue.offer(_)
    }

    def "handleStatus should handle offline operation"() {
        given:
        def integrationId = "test-integration"
        def device = createDevice(1L, integrationId, "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def config = DeviceStatusConfig.builder().build()
        def availableDeviceData = DeviceStatusService.AvailableDeviceData.of(device, config)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "ONLINE"

        when:
        deviceStatusService.handleStatus(1L, availableDeviceData, DeviceStatusService.DeviceStatusOperation.OFFLINE, true)

        then:
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    // ==================== getAvailableDeviceDataByDeviceId tests ====================

    def "getAvailableDeviceDataByDeviceId returns data with null device when device not found"() {
        given:
        deviceServiceProvider.findById(1L) >> null

        when:
        def result = deviceStatusService.getAvailableDeviceDataByDeviceId(1L)

        then:
        result != null
        result.device == null
        result.deviceStatusConfig == null
    }

    def "getAvailableDeviceDataByDeviceId returns data with null config when config not registered"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        deviceServiceProvider.findById(1L) >> device

        when:
        def result = deviceStatusService.getAvailableDeviceDataByDeviceId(1L)

        then:
        result != null
        result.device == device
        result.deviceStatusConfig == null
    }

    def "getAvailableDeviceDataByDeviceId returns data when device and config exist"() {
        given:
        def integrationId = "test-integration"
        def device = createDevice(1L, integrationId, "device-1")
        def config = DeviceStatusConfig.builder().build()

        deviceServiceProvider.findAll(integrationId) >> []
        deviceStatusService.register(integrationId, config)
        deviceServiceProvider.findById(1L) >> device

        when:
        def result = deviceStatusService.getAvailableDeviceDataByDeviceId(1L)

        then:
        result != null
        result.device == device
        result.deviceStatusConfig == config
    }

    // ==================== AvailableDeviceData tests ====================

    def "AvailableDeviceData.of creates instance correctly"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        def config = DeviceStatusConfig.builder().build()

        when:
        def result = DeviceStatusService.AvailableDeviceData.of(device, config)

        then:
        result.device == device
        result.deviceStatusConfig == config
    }

    // ==================== DeviceStatusOperation tests ====================

    @Unroll
    def "DeviceStatusOperation enum contains #operation"() {
        expect:
        DeviceStatusService.DeviceStatusOperation.valueOf(operation) != null

        where:
        operation << ["ONLINE", "OFFLINE"]
    }

    // ==================== edge case tests ====================

    def "should not update status when existing value is same as new status"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "ONLINE"

        when:
        deviceStatusService.online(device)

        then:
        0 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    def "should not update to offline when existing value is null and force is false"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def availableDeviceData = DeviceStatusService.AvailableDeviceData.of(device, null)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> null

        when:
        deviceStatusService.handleStatus(1L, availableDeviceData, DeviceStatusService.DeviceStatusOperation.OFFLINE, false)

        then:
        0 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    def "should update to offline when existing value is null and force is true"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def entityTemplate = Mock(EntityTemplate)
        def entity = Mock(Entity)

        entityServiceProvider.findByKey(statusEntityKey) >> null
        deviceServiceProvider.existsById(1L) >> true
        entityTemplateServiceProvider.findByKey(DeviceStatusConstants.IDENTIFIER_DEVICE_STATUS) >> entityTemplate
        entityTemplate.toEntity("test-integration", device.getKey()) >> entity
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> null

        when:
        deviceStatusService.offline(device)

        then:
        1 * entityServiceProvider.save(entity)
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    def "should throw exception when entity template not found"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)

        entityServiceProvider.findByKey(statusEntityKey) >> null
        deviceServiceProvider.existsById(1L) >> true
        entityTemplateServiceProvider.findByKey(DeviceStatusConstants.IDENTIFIER_DEVICE_STATUS) >> null

        when:
        deviceStatusService.online(device)

        then:
        thrown(RuntimeException)
    }

    def "should skip update when device not exists"() {
        given:
        def device = createDevice(1L, "test-integration", "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)

        entityServiceProvider.findByKey(statusEntityKey) >> null
        deviceServiceProvider.existsById(1L) >> false

        when:
        deviceStatusService.online(device)

        then:
        0 * entityTemplateServiceProvider.findByKey(_)
        0 * entityServiceProvider.save(_)
        0 * entityValueServiceProvider.saveValuesAndPublishSync(_)
    }

    def "should call online listener when status changes to online"() {
        given:
        def integrationId = "test-integration"
        def device = createDevice(1L, integrationId, "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def onlineListenerCalled = false
        def config = DeviceStatusConfig.builder()
                .onlineListener({ d -> onlineListenerCalled = true })
                .offlineTimeoutFetcher({ d -> Duration.ofMinutes(5) })
                .build()
        def availableDeviceData = DeviceStatusService.AvailableDeviceData.of(device, config)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "OFFLINE"

        when:
        deviceStatusService.handleStatus(1L, availableDeviceData, DeviceStatusService.DeviceStatusOperation.ONLINE, true)

        then:
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
        onlineListenerCalled
    }

    def "should skip delayed task when offline duration is null"() {
        given:
        def integrationId = "test-integration"
        def device = createDevice(1L, integrationId, "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def config = DeviceStatusConfig.builder()
                .offlineTimeoutFetcher({ d -> null })
                .build()
        def availableDeviceData = DeviceStatusService.AvailableDeviceData.of(device, config)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "OFFLINE"

        when:
        deviceStatusService.handleStatus(1L, availableDeviceData, DeviceStatusService.DeviceStatusOperation.ONLINE, true)

        then:
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
        0 * delayedQueue.offer(_)
    }

    def "should skip delayed task when offline duration is zero or negative"() {
        given:
        def integrationId = "test-integration"
        def device = createDevice(1L, integrationId, "device-1")
        def statusEntityKey = getDeviceStatusEntityKey(device)
        def config = DeviceStatusConfig.builder()
                .offlineTimeoutFetcher({ d -> Duration.ZERO })
                .build()
        def availableDeviceData = DeviceStatusService.AvailableDeviceData.of(device, config)

        entityServiceProvider.findByKey(statusEntityKey) >> Mock(Entity)
        entityValueServiceProvider.findValueByKey(statusEntityKey) >> "OFFLINE"

        when:
        deviceStatusService.handleStatus(1L, availableDeviceData, DeviceStatusService.DeviceStatusOperation.ONLINE, true)

        then:
        1 * entityValueServiceProvider.saveValuesAndPublishSync(_)
        0 * delayedQueue.offer(_)
    }

    // ==================== helper methods ====================

    private Device createDevice(Long id, String integrationId, String identifier) {
        def device = Mock(Device)
        device.getId() >> id
        device.getIntegrationId() >> integrationId
        device.getKey() >> integrationId + ".device." +  identifier
        device.getName() >> "Device " + identifier
        return device
    }

    private static String getDeviceStatusEntityKey(Device device) {
        return device.getKey() + ".@status"
    }
}
