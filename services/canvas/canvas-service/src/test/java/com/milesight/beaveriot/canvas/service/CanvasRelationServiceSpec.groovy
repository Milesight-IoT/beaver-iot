package com.milesight.beaveriot.canvas.service

import com.milesight.beaveriot.canvas.po.CanvasDevicePO
import com.milesight.beaveriot.canvas.po.CanvasEntityPO
import com.milesight.beaveriot.canvas.repository.CanvasDeviceRepository
import com.milesight.beaveriot.canvas.repository.CanvasEntityRepository
import com.milesight.beaveriot.context.api.EntityServiceProvider
import com.milesight.beaveriot.device.dto.DeviceIdKeyDTO
import com.milesight.beaveriot.device.facade.IDeviceFacade
import com.milesight.beaveriot.entity.dto.EntityIdKeyDTO
import com.milesight.beaveriot.entity.facade.IEntityFacade
import spock.lang.Specification

/**
 * Test class for CanvasRelationService
 *
 * @author simon
 * @date 2025/11/21
 */
class CanvasRelationServiceSpec extends Specification {

    CanvasRelationService canvasRelationService
    CanvasEntityRepository canvasEntityRepository = Mock()
    CanvasDeviceRepository canvasDeviceRepository = Mock()
    EntityServiceProvider entityServiceProvider = Mock()
    IDeviceFacade deviceFacade = Mock()
    IEntityFacade entityFacade = Mock()

    def setup() {
        canvasRelationService = new CanvasRelationService()
        canvasRelationService.canvasEntityRepository = canvasEntityRepository
        canvasRelationService.canvasDeviceRepository = canvasDeviceRepository
        canvasRelationService.entityServiceProvider = entityServiceProvider
        canvasRelationService.deviceFacade = deviceFacade
        canvasRelationService.entityFacade = entityFacade
    }

    // ==================== saveCanvasEntities tests ====================

    def "saveCanvasEntities should handle empty entity ID list"() {
        given:
        def canvasId = 1L
        def entityIdList = []

        when:
        canvasRelationService.saveCanvasEntities(canvasId, entityIdList)

        then:
        1 * canvasEntityRepository.deleteAllByCanvasId(canvasId)
        1 * canvasEntityRepository.flush()
        0 * entityFacade.findIdAndKeyByIds(_)
        0 * canvasEntityRepository.findAll(_)
        0 * canvasEntityRepository.saveAll(_)
    }

    def "saveCanvasEntities should handle null entity ID list"() {
        given:
        def canvasId = 1L
        def entityIdList = null

        when:
        canvasRelationService.saveCanvasEntities(canvasId, entityIdList)

        then:
        1 * canvasEntityRepository.deleteAllByCanvasId(canvasId)
        1 * canvasEntityRepository.flush()
        0 * entityFacade.findIdAndKeyByIds(_)
        0 * canvasEntityRepository.findAll(_)
        0 * canvasEntityRepository.saveAll(_)
    }

    // ==================== Differential update tests ====================

    def "saveCanvasEntities should handle partial overlap - some kept, some deleted, some added"() {
        given:
        def canvasId = 1L
        def newEntityIdList = [200L, 300L, 400L] // Keep 200L, delete 100L, add 300L and 400L
        def validatedEntities = [
                createEntityIdKeyDTO(200L, "entity-key-2"),
                createEntityIdKeyDTO(300L, "entity-key-3"),
                createEntityIdKeyDTO(400L, "entity-key-4")
        ]
        def existingRelations = [
                createCanvasEntityPO(1L, canvasId, 100L, "entity-key-1"),
                createCanvasEntityPO(2L, canvasId, 200L, "entity-key-2")
        ]

        when:
        canvasRelationService.saveCanvasEntities(canvasId, newEntityIdList)

        then:
        1 * entityFacade.findIdAndKeyByIds(newEntityIdList) >> validatedEntities
        1 * canvasEntityRepository.findAll(_) >> existingRelations
        1 * canvasEntityRepository.deleteAllByCanvasIdAndEntityIdIn(canvasId, [100L])
        1 * canvasEntityRepository.flush()
        1 * canvasEntityRepository.saveAll({ List<CanvasEntityPO> saved ->
            saved.size() == 2 &&
            saved.find { it.entityId == 300L && it.entityKey == "entity-key-3" } != null &&
            saved.find { it.entityId == 400L && it.entityKey == "entity-key-4" } != null
        })
    }

    def "saveCanvasEntities should handle no overlap - delete all old, add all new"() {
        given:
        def canvasId = 1L
        def newEntityIdList = [300L, 400L]
        def validatedEntities = [
                createEntityIdKeyDTO(300L, "entity-key-3"),
                createEntityIdKeyDTO(400L, "entity-key-4")
        ]
        def existingRelations = [
                createCanvasEntityPO(1L, canvasId, 100L, "entity-key-1"),
                createCanvasEntityPO(2L, canvasId, 200L, "entity-key-2")
        ]

        when:
        canvasRelationService.saveCanvasEntities(canvasId, newEntityIdList)

        then:
        1 * entityFacade.findIdAndKeyByIds(newEntityIdList) >> validatedEntities
        1 * canvasEntityRepository.findAll(_) >> existingRelations
        1 * canvasEntityRepository.deleteAllByCanvasIdAndEntityIdIn(canvasId, [100L, 200L])
        1 * canvasEntityRepository.flush()
        1 * canvasEntityRepository.saveAll({ List<CanvasEntityPO> saved ->
            saved.size() == 2 &&
            saved.find { it.entityId == 300L } != null &&
            saved.find { it.entityId == 400L } != null
        })
    }

    def "saveCanvasEntities should handle complete overlap - no changes needed"() {
        given:
        def canvasId = 1L
        def newEntityIdList = [100L, 200L]
        def validatedEntities = [
                createEntityIdKeyDTO(100L, "entity-key-1"),
                createEntityIdKeyDTO(200L, "entity-key-2")
        ]
        def existingRelations = [
                createCanvasEntityPO(1L, canvasId, 100L, "entity-key-1"),
                createCanvasEntityPO(2L, canvasId, 200L, "entity-key-2")
        ]

        when:
        canvasRelationService.saveCanvasEntities(canvasId, newEntityIdList)

        then:
        1 * entityFacade.findIdAndKeyByIds(newEntityIdList) >> validatedEntities
        1 * canvasEntityRepository.findAll(_) >> existingRelations
        0 * canvasEntityRepository.deleteAllByCanvasId(_)
        0 * canvasEntityRepository.deleteAllByCanvasIdAndEntityIdIn(_, _)
        0 * canvasEntityRepository.flush()
        0 * canvasEntityRepository.saveAll(_)
    }

    def "saveCanvasEntities should handle delete-only scenario"() {
        given:
        def canvasId = 1L
        def newEntityIdList = [100L] // Keep only 100L, delete 200L and 300L
        def validatedEntities = [
                createEntityIdKeyDTO(100L, "entity-key-1")
        ]
        def existingRelations = [
                createCanvasEntityPO(1L, canvasId, 100L, "entity-key-1"),
                createCanvasEntityPO(2L, canvasId, 200L, "entity-key-2"),
                createCanvasEntityPO(3L, canvasId, 300L, "entity-key-3")
        ]

        when:
        canvasRelationService.saveCanvasEntities(canvasId, newEntityIdList)

        then:
        1 * entityFacade.findIdAndKeyByIds(newEntityIdList) >> validatedEntities
        1 * canvasEntityRepository.findAll(_) >> existingRelations
        1 * canvasEntityRepository.deleteAllByCanvasIdAndEntityIdIn(canvasId, [200L, 300L])
        1 * canvasEntityRepository.flush()
        0 * canvasEntityRepository.saveAll(_)
    }

    def "saveCanvasEntities should handle add-only scenario"() {
        given:
        def canvasId = 1L
        def newEntityIdList = [100L, 200L, 300L] // Keep 100L and 200L, add 300L
        def validatedEntities = [
                createEntityIdKeyDTO(100L, "entity-key-1"),
                createEntityIdKeyDTO(200L, "entity-key-2"),
                createEntityIdKeyDTO(300L, "entity-key-3")
        ]
        def existingRelations = [
                createCanvasEntityPO(1L, canvasId, 100L, "entity-key-1"),
                createCanvasEntityPO(2L, canvasId, 200L, "entity-key-2")
        ]

        when:
        canvasRelationService.saveCanvasEntities(canvasId, newEntityIdList)

        then:
        1 * entityFacade.findIdAndKeyByIds(newEntityIdList) >> validatedEntities
        1 * canvasEntityRepository.findAll(_) >> existingRelations
        0 * canvasEntityRepository.deleteAllByCanvasId(_)
        0 * canvasEntityRepository.deleteAllByCanvasIdAndEntityIdIn(_, _)
        0 * canvasEntityRepository.flush()
        1 * canvasEntityRepository.saveAll({ List<CanvasEntityPO> saved ->
            saved.size() == 1 &&
            saved[0].entityId == 300L &&
            saved[0].entityKey == "entity-key-3"
        })
    }

    // ==================== saveCanvasDevices tests ====================

    def "saveCanvasDevices should handle empty device ID list"() {
        given:
        def canvasId = 1L
        def deviceIdList = []

        when:
        canvasRelationService.saveCanvasDevices(canvasId, deviceIdList)

        then:
        1 * canvasDeviceRepository.deleteAllByCanvasIdIn([canvasId])
        1 * canvasDeviceRepository.flush()
        0 * deviceFacade.findIdAndKeyByIds(_)
        0 * canvasDeviceRepository.findAll(_)
        0 * canvasDeviceRepository.saveAll(_)
    }

    def "saveCanvasDevices should handle null device ID list"() {
        given:
        def canvasId = 1L
        def deviceIdList = null

        when:
        canvasRelationService.saveCanvasDevices(canvasId, deviceIdList)

        then:
        1 * canvasDeviceRepository.deleteAllByCanvasIdIn([canvasId])
        1 * canvasDeviceRepository.flush()
        0 * deviceFacade.findIdAndKeyByIds(_)
        0 * canvasDeviceRepository.findAll(_)
        0 * canvasDeviceRepository.saveAll(_)
    }

    // ==================== Differential update tests for devices ====================

    def "saveCanvasDevices should handle partial overlap - some kept, some deleted, some added"() {
        given:
        def canvasId = 1L
        def newDeviceIdList = [200L, 300L, 400L] // Keep 200L, delete 100L, add 300L and 400L
        def validatedDevices = [
                createDeviceIdKeyDTO(200L, "device-key-2"),
                createDeviceIdKeyDTO(300L, "device-key-3"),
                createDeviceIdKeyDTO(400L, "device-key-4")
        ]
        def existingRelations = [
                createCanvasDevicePO(1L, canvasId, 100L),
                createCanvasDevicePO(2L, canvasId, 200L)
        ]

        when:
        canvasRelationService.saveCanvasDevices(canvasId, newDeviceIdList)

        then:
        1 * deviceFacade.findIdAndKeyByIds(newDeviceIdList) >> validatedDevices
        1 * canvasDeviceRepository.findAll(_) >> existingRelations
        1 * canvasDeviceRepository.deleteAllByCanvasIdAndDeviceIdIn(canvasId, [100L])
        1 * canvasDeviceRepository.flush()
        1 * canvasDeviceRepository.saveAll({ List<CanvasDevicePO> saved ->
            saved.size() == 2 &&
            saved.find { it.deviceId == 300L } != null &&
            saved.find { it.deviceId == 400L } != null
        })
    }

    def "saveCanvasDevices should handle no overlap - delete all old, add all new"() {
        given:
        def canvasId = 1L
        def newDeviceIdList = [300L, 400L]
        def validatedDevices = [
                createDeviceIdKeyDTO(300L, "device-key-3"),
                createDeviceIdKeyDTO(400L, "device-key-4")
        ]
        def existingRelations = [
                createCanvasDevicePO(1L, canvasId, 100L),
                createCanvasDevicePO(2L, canvasId, 200L)
        ]

        when:
        canvasRelationService.saveCanvasDevices(canvasId, newDeviceIdList)

        then:
        1 * deviceFacade.findIdAndKeyByIds(newDeviceIdList) >> validatedDevices
        1 * canvasDeviceRepository.findAll(_) >> existingRelations
        1 * canvasDeviceRepository.deleteAllByCanvasIdAndDeviceIdIn(canvasId, [100L, 200L])
        1 * canvasDeviceRepository.flush()
        1 * canvasDeviceRepository.saveAll({ List<CanvasDevicePO> saved ->
            saved.size() == 2 &&
            saved.find { it.deviceId == 300L } != null &&
            saved.find { it.deviceId == 400L } != null
        })
    }

    def "saveCanvasDevices should handle complete overlap - no changes needed"() {
        given:
        def canvasId = 1L
        def newDeviceIdList = [100L, 200L]
        def validatedDevices = [
                createDeviceIdKeyDTO(100L, "device-key-1"),
                createDeviceIdKeyDTO(200L, "device-key-2")
        ]
        def existingRelations = [
                createCanvasDevicePO(1L, canvasId, 100L),
                createCanvasDevicePO(2L, canvasId, 200L)
        ]

        when:
        canvasRelationService.saveCanvasDevices(canvasId, newDeviceIdList)

        then:
        1 * deviceFacade.findIdAndKeyByIds(newDeviceIdList) >> validatedDevices
        1 * canvasDeviceRepository.findAll(_) >> existingRelations
        0 * canvasDeviceRepository.deleteAllByCanvasIdIn(_)
        0 * canvasDeviceRepository.deleteAllByCanvasIdAndDeviceIdIn(_, _)
        0 * canvasDeviceRepository.flush()
        0 * canvasDeviceRepository.saveAll(_)
    }

    def "saveCanvasDevices should handle delete-only scenario"() {
        given:
        def canvasId = 1L
        def newDeviceIdList = [100L] // Keep only 100L, delete 200L and 300L
        def validatedDevices = [
                createDeviceIdKeyDTO(100L, "device-key-1")
        ]
        def existingRelations = [
                createCanvasDevicePO(1L, canvasId, 100L),
                createCanvasDevicePO(2L, canvasId, 200L),
                createCanvasDevicePO(3L, canvasId, 300L)
        ]

        when:
        canvasRelationService.saveCanvasDevices(canvasId, newDeviceIdList)

        then:
        1 * deviceFacade.findIdAndKeyByIds(newDeviceIdList) >> validatedDevices
        1 * canvasDeviceRepository.findAll(_) >> existingRelations
        1 * canvasDeviceRepository.deleteAllByCanvasIdAndDeviceIdIn(canvasId, [200L, 300L])
        1 * canvasDeviceRepository.flush()
        0 * canvasDeviceRepository.saveAll(_)
    }

    def "saveCanvasDevices should handle add-only scenario"() {
        given:
        def canvasId = 1L
        def newDeviceIdList = [100L, 200L, 300L] // Keep 100L and 200L, add 300L
        def validatedDevices = [
                createDeviceIdKeyDTO(100L, "device-key-1"),
                createDeviceIdKeyDTO(200L, "device-key-2"),
                createDeviceIdKeyDTO(300L, "device-key-3")
        ]
        def existingRelations = [
                createCanvasDevicePO(1L, canvasId, 100L),
                createCanvasDevicePO(2L, canvasId, 200L)
        ]

        when:
        canvasRelationService.saveCanvasDevices(canvasId, newDeviceIdList)

        then:
        1 * deviceFacade.findIdAndKeyByIds(newDeviceIdList) >> validatedDevices
        1 * canvasDeviceRepository.findAll(_) >> existingRelations
        0 * canvasDeviceRepository.deleteAllByCanvasIdIn(_)
        0 * canvasDeviceRepository.deleteAllByCanvasIdAndDeviceIdIn(_, _)
        0 * canvasDeviceRepository.flush()
        1 * canvasDeviceRepository.saveAll({ List<CanvasDevicePO> saved ->
            saved.size() == 1 &&
            saved[0].deviceId == 300L
        })
    }

    // ==================== Helper methods ====================

    private static EntityIdKeyDTO createEntityIdKeyDTO(Long id, String key) {
        return EntityIdKeyDTO.builder()
                .id(id)
                .key(key)
                .build()
    }

    private static DeviceIdKeyDTO createDeviceIdKeyDTO(Long id, String key) {
        return DeviceIdKeyDTO.builder()
                .id(id)
                .key(key)
                .build()
    }

    private static CanvasEntityPO createCanvasEntityPO(Long id, Long canvasId, Long entityId, String entityKey) {
        return CanvasEntityPO.builder()
                .id(id)
                .canvasId(canvasId)
                .entityId(entityId)
                .entityKey(entityKey)
                .build()
    }

    private static CanvasDevicePO createCanvasDevicePO(Long id, Long canvasId, Long deviceId) {
        return CanvasDevicePO.builder()
                .id(id)
                .canvasId(canvasId)
                .deviceId(deviceId)
                .build()
    }
}
