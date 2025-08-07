package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.base.annotations.shedlock.DistributedLock;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.device.constants.DeviceDataFieldConstants;
import com.milesight.beaveriot.device.enums.DeviceErrorCode;
import com.milesight.beaveriot.device.model.request.CreateDeviceGroupRequest;
import com.milesight.beaveriot.device.model.request.SearchDeviceGroupRequest;
import com.milesight.beaveriot.device.model.response.DeviceGroupResponseData;
import com.milesight.beaveriot.device.po.DeviceGroupMappingPO;
import com.milesight.beaveriot.device.po.DeviceGroupPO;
import com.milesight.beaveriot.device.repository.DeviceGroupMappingRepository;
import com.milesight.beaveriot.device.repository.DeviceGroupRepository;
import lombok.extern.slf4j.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * DeviceGroupService class.
 *
 * @author simon
 * @date 2025/6/25
 */
@Service
@Slf4j
public class DeviceGroupService {
    @Autowired
    DeviceGroupRepository deviceGroupRepository;

    @Autowired
    DeviceGroupMappingRepository deviceGroupMappingRepository;

    @DistributedLock(name = "device-group-#{#p0.name}", waitForLock = "5s")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DeviceGroupPO getOrCreateDeviceGroup(CreateDeviceGroupRequest request, boolean mustCreate) {
        Optional<DeviceGroupPO> deviceGroupPOOptional = deviceGroupRepository.findOne(f -> f.eq(DeviceGroupPO.Fields.name, request.getName()));
        if (deviceGroupPOOptional.isPresent()) {
            if (mustCreate) {
                throw ServiceException.with(DeviceErrorCode.DEVICE_GROUP_NAME_EXISTS).build();
            }

            return deviceGroupPOOptional.get();
        }

        if (deviceGroupRepository.count() >= DeviceDataFieldConstants.DEVICE_GROUP_MAX_NUMBER) {
            throw ServiceException.with(DeviceErrorCode.DEVICE_GROUP_NUMBER_LIMITATION).build();
        }

        DeviceGroupPO deviceGroupPO = new DeviceGroupPO();
        deviceGroupPO.setId(SnowflakeUtil.nextId());
        deviceGroupPO.setName(request.getName());
        deviceGroupRepository.save(deviceGroupPO);
        return deviceGroupPO;
    }

    public Long countDeviceGroup() {
        return deviceGroupRepository.count();
    }

    private DeviceGroupResponseData mapToResponseData(DeviceGroupPO deviceGroupPO) {
        DeviceGroupResponseData responseData = new DeviceGroupResponseData();
        responseData.setId(deviceGroupPO.getId().toString());
        responseData.setName(deviceGroupPO.getName());
        return responseData;
    }

    public Page<DeviceGroupResponseData> search(SearchDeviceGroupRequest request) {
        Consumer<Filterable> filterable = f -> f.likeIgnoreCase(StringUtils.hasText(request.getName()), DeviceGroupPO.Fields.name, request.getName());
        if (request.getSort().getOrders().isEmpty()) {
            request.sort(new Sorts().desc(DeviceGroupPO.Fields.id));
        }

        return deviceGroupRepository.findAll(filterable, request.toPageable()).map(this::mapToResponseData);
    }

    public List<Long> findAllGroupedDeviceIdList() {
        return deviceGroupMappingRepository.findAllGroupedDeviceIdList();
    }

    public DeviceGroupPO getDeviceGroup(Long deviceGroupId) {
        DeviceGroupPO deviceGroupPO = deviceGroupRepository.findById(deviceGroupId).orElse(null);
        if (deviceGroupPO == null) {
            throw ServiceException.with(DeviceErrorCode.DEVICE_GROUP_NOT_FOUND).build();
        }

        return deviceGroupPO;
    }

    public void updateDeviceGroup(Long id, CreateDeviceGroupRequest request) {
        DeviceGroupPO deviceGroupPO = getDeviceGroup(id);
        if (!Objects.equals(deviceGroupPO.getName(), request.getName())) {
            Optional<DeviceGroupPO> deviceGroupPOOptional = deviceGroupRepository.findOne(f -> f.eq(DeviceGroupPO.Fields.name, request.getName()));
            if (deviceGroupPOOptional.isPresent()) {
                throw ServiceException.with(DeviceErrorCode.DEVICE_GROUP_NAME_EXISTS).build();
            }

            deviceGroupPO.setName(request.getName());
        }

        deviceGroupRepository.save(deviceGroupPO);
    }

    @Transactional
    public void deleteDeviceGroup(Long id) {
        DeviceGroupPO deviceGroupPO = getDeviceGroup(id);

        deviceGroupMappingRepository.deleteAllByGroupId(deviceGroupPO.getId());

        deviceGroupRepository.delete(deviceGroupPO);
    }

    public Map<Long, DeviceGroupPO> deviceMapToGroup(List<Long> deviceIdList) {
        if (deviceIdList == null || deviceIdList.isEmpty()) {
            return Map.of();
        }

        List<DeviceGroupMappingPO> mappings = deviceGroupMappingRepository
                .findAllByDeviceIdIn(deviceIdList);
        if (mappings.isEmpty()) {
            return Map.of();
        }

        List<Long> groupIdList = mappings
                .stream().map(DeviceGroupMappingPO::getGroupId)
                .distinct()
                .toList();

        Map<Long, DeviceGroupPO> groups = deviceGroupRepository
                .findAllById(groupIdList)
                .stream()
                .collect(Collectors.toMap(DeviceGroupPO::getId, Function.identity()));
        Map<Long, DeviceGroupPO> result = new HashMap<>();
        mappings.forEach(mapping -> {
            DeviceGroupPO group = groups.get(mapping.getGroupId());
            if (group != null) {
                result.put(mapping.getDeviceId(), group);
            }
        });
        return result;
    }

    @Transactional
    public void removeDevices(List<Long> deviceIdList) {
        if (deviceIdList == null || deviceIdList.isEmpty()) {
            return;
        }

        List<Long> mappingIdList = deviceGroupMappingRepository
                .findAllByDeviceIdIn(deviceIdList)
                .stream().map(DeviceGroupMappingPO::getId)
                .toList();

        if (mappingIdList.isEmpty()) {
            return;
        }

        deviceGroupMappingRepository.deleteAllById(mappingIdList);
    }

    public void moveDevicesToGroupId(Long targetGroupId, List<Long> deviceIdList) {
        if (deviceIdList == null || deviceIdList.isEmpty()) {
            return;
        }

        Set<Long> deviceIdListToAdd = new HashSet<>(deviceIdList);
        List<DeviceGroupMappingPO> mappingListToSave = new ArrayList<>();

        // update old
        deviceGroupMappingRepository.findAllByDeviceIdIn(deviceIdList).forEach(mappingPO -> {
            deviceIdListToAdd.remove(mappingPO.getDeviceId());
            if (!Objects.equals(mappingPO.getGroupId(), targetGroupId)) {
                mappingPO.setGroupId(targetGroupId);
                mappingListToSave.add(mappingPO);
            }
        });

        // create new
        deviceIdListToAdd.forEach(deviceId -> {
            DeviceGroupMappingPO mappingPO = new DeviceGroupMappingPO();
            mappingPO.setId(SnowflakeUtil.nextId());
            mappingPO.setDeviceId(deviceId);
            mappingPO.setGroupId(targetGroupId);
            mappingListToSave.add(mappingPO);
        });

        deviceGroupMappingRepository.saveAll(mappingListToSave);
    }

    public List<DeviceGroupMappingPO> findAllMappingByGroupId(Long groupId) {
        return deviceGroupMappingRepository.findAll(f -> f.eq(DeviceGroupMappingPO.Fields.groupId, groupId));
    }

    public Map<Long, List<DeviceGroupPO>> deviceIdToGroups(List<Long> deviceIds) {
        List<DeviceGroupMappingPO> mappings = deviceGroupMappingRepository.findAllByDeviceIdIn(deviceIds);
        Map<Long, Set<Long>> groupIdToDeviceIds = mappings.stream()
                .collect(Collectors.groupingBy(DeviceGroupMappingPO::getGroupId, Collectors.mapping(DeviceGroupMappingPO::getDeviceId, Collectors.toSet())));
        if (groupIdToDeviceIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, DeviceGroupPO> groupIdToPO = deviceGroupRepository.findAllById(groupIdToDeviceIds.keySet()).stream()
                .collect(Collectors.toMap(DeviceGroupPO::getId, Function.identity(), (a, b) -> a));
        return groupIdToDeviceIds.entrySet().stream()
                .flatMap(entry -> {
                    DeviceGroupPO group = groupIdToPO.get(entry.getKey());
                    if (group == null) {
                        return Stream.empty();
                    }
                    return entry.getValue().stream()
                            .map(deviceId -> Pair.of(deviceId, group));
                })
                .collect(Collectors.groupingBy(Pair::getFirst, Collectors.mapping(Pair::getSecond, Collectors.toList())));
    }

    public List<Long> findAllDeviceIdsByGroupNameIn(List<String> groupNames) {
        List<Long> groupIds = deviceGroupRepository.findAll(f -> f.in(DeviceGroupPO.Fields.name, groupNames.toArray()))
                .stream()
                .map(DeviceGroupPO::getId)
                .toList();
        return deviceGroupMappingRepository.findAll(f -> f.in(DeviceGroupMappingPO.Fields.groupId, groupIds.toArray()))
                .stream()
                .map(DeviceGroupMappingPO::getDeviceId)
                .collect(Collectors.toList());
    }

}
