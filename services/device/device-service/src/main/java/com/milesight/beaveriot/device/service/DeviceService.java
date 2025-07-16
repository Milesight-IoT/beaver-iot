package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheEvict;
import com.milesight.beaveriot.base.annotations.cacheable.BatchCacheable;
import com.milesight.beaveriot.base.annotations.cacheable.CacheKeys;
import com.milesight.beaveriot.base.enums.ComparisonOperator;
import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.EntityValueServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.constants.CacheKeyConstants;
import com.milesight.beaveriot.context.integration.enums.AttachTargetType;
import com.milesight.beaveriot.context.integration.model.Device;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.context.integration.model.event.DeviceEvent;
import com.milesight.beaveriot.context.model.EntityTag;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.data.filterable.Filterable;
import com.milesight.beaveriot.device.dto.DeviceNameDTO;
import com.milesight.beaveriot.device.facade.IDeviceFacade;
import com.milesight.beaveriot.device.model.request.CreateDeviceRequest;
import com.milesight.beaveriot.device.model.request.MoveDeviceToGroupRequest;
import com.milesight.beaveriot.device.model.request.SearchDeviceRequest;
import com.milesight.beaveriot.device.model.request.UpdateDeviceRequest;
import com.milesight.beaveriot.device.model.response.DeviceDetailResponse;
import com.milesight.beaveriot.device.model.response.DeviceEntityData;
import com.milesight.beaveriot.device.model.response.DeviceResponseData;
import com.milesight.beaveriot.device.po.DeviceGroupMappingPO;
import com.milesight.beaveriot.device.po.DeviceGroupPO;
import com.milesight.beaveriot.device.po.DevicePO;
import com.milesight.beaveriot.device.repository.DeviceRepository;
import com.milesight.beaveriot.device.support.DeviceConverter;
import com.milesight.beaveriot.devicetemplate.dto.DeviceTemplateDTO;
import com.milesight.beaveriot.devicetemplate.facade.IDeviceTemplateFacade;
import com.milesight.beaveriot.eventbus.EventBus;
import com.milesight.beaveriot.permission.aspect.IntegrationPermission;
import com.milesight.beaveriot.user.dto.UserDTO;
import com.milesight.beaveriot.user.enums.ResourceType;
import com.milesight.beaveriot.user.facade.IUserFacade;
import lombok.extern.slf4j.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.DEVICE_NAME_ON_ADD;
import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.DEVICE_ON_DELETE;
import static com.milesight.beaveriot.context.constants.ExchangeContextKeys.DEVICE_TEMPLATE_KEY_ON_ADD;

@Service
@Slf4j
public class DeviceService implements IDeviceFacade {

    @Autowired
    private DeviceRepository deviceRepository;

    @Lazy
    @Autowired
    private IntegrationServiceProvider integrationServiceProvider;

    @Lazy
    @Autowired
    private DeviceConverter deviceConverter;

    @Lazy
    @Autowired
    private EntityServiceProvider entityServiceProvider;

    @Autowired
    private IUserFacade userFacade;

    @Autowired
    private IDeviceTemplateFacade deviceTemplateFacade;

    @Autowired
    private EventBus<DeviceEvent> eventBus;

    @Autowired
    private EntityValueServiceProvider entityValueServiceProvider;

    @Autowired
    DeviceGroupService deviceGroupService;

    @Lazy
    @Autowired
    private DeviceService self;

    public static final String TENANT_PARAM_DEVICE_GROUP_NAME = "DEVICE_GROUP_NAME";

    @IntegrationPermission
    public Integration getIntegration(String integrationIdentifier) {
        return integrationServiceProvider.getIntegration(integrationIdentifier);
    }

    @Transactional(rollbackFor = Exception.class)
    public void createDevice(CreateDeviceRequest createDeviceRequest) {
        String integrationIdentifier = createDeviceRequest.getIntegration();
        Integration integrationConfig = self.getIntegration(integrationIdentifier);

        if (integrationConfig == null) {
            throw ServiceException
                    .with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "integration " + integrationIdentifier + " not found!")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        // check ability to add device
        String addDeviceEntityId = integrationConfig.getEntityIdentifierAddDevice();
        if (addDeviceEntityId == null) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "integration " + integrationIdentifier + " cannot add device!")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .build();
        }

        ExchangePayload payload = createDeviceRequest.getParamEntities();
        payload.validate();
        payload.putContext(DEVICE_NAME_ON_ADD, createDeviceRequest.getName());
        payload.putContext(DEVICE_TEMPLATE_KEY_ON_ADD, createDeviceRequest.getTemplate());

        boolean hasGroup = StringUtils.hasText(createDeviceRequest.getGroupName());
        TenantContext.tryPutTenantParam(TENANT_PARAM_DEVICE_GROUP_NAME, hasGroup ? createDeviceRequest.getGroupName() : null);

        try {
            // call service for adding
            entityValueServiceProvider.saveValuesAndPublishSync(payload);
        } finally {
            if (hasGroup) {
                TenantContext.tryPutTenantParam(TENANT_PARAM_DEVICE_GROUP_NAME, null);
            }
        }
    }

    private DeviceResponseData convertPOToResponseData(DevicePO devicePO) {
        DeviceResponseData deviceResponseData = new DeviceResponseData();
        deviceResponseData.setId(devicePO.getId().toString());
        deviceResponseData.setKey(devicePO.getKey());
        deviceResponseData.setName(devicePO.getName());
        deviceResponseData.setIntegration(devicePO.getIntegration());
        deviceResponseData.setIdentifier(devicePO.getIdentifier());
        deviceResponseData.setAdditionalData(devicePO.getAdditionalData());
        deviceResponseData.setTemplate(devicePO.getTemplate());
        deviceResponseData.setCreatedAt(devicePO.getCreatedAt());
        deviceResponseData.setUpdatedAt(devicePO.getUpdatedAt());

        return deviceResponseData;
    }

    private Map<String, Integration> getIntegrationMap(List<String> identifiers) {
        Set<String> integrationIdentifiers = new HashSet<>(identifiers);
        return integrationServiceProvider
                .findIntegrations(f -> integrationIdentifiers.contains(f.getId()))
                .stream()
                .collect(Collectors.toMap(Integration::getId, integration -> integration));
    }


    private void fillRelativeInfo(List<DeviceResponseData> dataList) {
        Map<String, Integration> integrationMap = getIntegrationMap(dataList.stream().map(DeviceResponseData::getIntegration).toList());
        Map<Long, DeviceGroupPO> deviceGroupMap = deviceGroupService.deviceMapToGroup(dataList.stream().map(d -> Long.valueOf(d.getId())).toList());

        dataList.forEach(d -> {
            Integration integration = integrationMap.get(d.getIntegration());
            if (integration == null) {
                d.setDeletable(true);
                return;
            }

            d.setDeletable(StringUtils.hasLength(integration.getEntityIdentifierDeleteDevice()));
            d.setIntegrationName(integration.getName());

            DeviceGroupPO groupPO = deviceGroupMap.get(Long.valueOf(d.getId()));
            if (groupPO != null) {
                d.setGroupName(groupPO.getName());
            }
        });
    }

    @Override
    public List<Long> fuzzySearchDeviceIdsByName(ComparisonOperator operator, String keyword) {
        Assert.hasText(keyword, "keyword cannot be empty");
        return deviceRepository.findAllWithDataPermission(f -> {
                    switch (operator) {
                        case EQ -> f.eq(DevicePO.Fields.name, keyword);
                        case NE -> f.ne(DevicePO.Fields.name, keyword);
                        case CONTAINS -> f.likeIgnoreCase(DevicePO.Fields.name, keyword);
                        case NOT_CONTAINS -> f.notLikeIgnoreCase(DevicePO.Fields.name, keyword);
                        case START_WITH -> f.startsWithIgnoreCase(DevicePO.Fields.name, keyword);
                        case END_WITH -> f.endsWithIgnoreCase(DevicePO.Fields.name, keyword);
                        default ->
                                throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED).detailMessage("Unsupported operator: " + operator).build();
                    }
                })
                .stream()
                .map(DevicePO::getId)
                .collect(Collectors.toList());
    }

    @Override
    public List<Long> findDeviceIdsByGroupNameIn(List<String> deviceGroupNames) {
        return deviceGroupService.findAllDeviceIdsByGroupNameIn(deviceGroupNames);
    }

    public Page<DeviceResponseData> searchDevice(SearchDeviceRequest searchDeviceRequest) {
        if (searchDeviceRequest.getSort().getOrders().isEmpty()) {
            searchDeviceRequest.sort(new Sorts().desc(DevicePO.Fields.id));
        }

        Consumer<Filterable> filter = f -> f.likeIgnoreCase(StringUtils.hasText(searchDeviceRequest.getName()), DevicePO.Fields.name, searchDeviceRequest.getName())
                .eq(StringUtils.hasText(searchDeviceRequest.getTemplate()), DevicePO.Fields.template, searchDeviceRequest.getTemplate())
                .likeIgnoreCase(StringUtils.hasText(searchDeviceRequest.getIdentifier()), DevicePO.Fields.identifier, searchDeviceRequest.getIdentifier());

        Page<DeviceResponseData> responseDataList = Page.empty();
        String groupIdStr = searchDeviceRequest.getGroupId();
        boolean filterNotGrouped = Boolean.TRUE.equals(searchDeviceRequest.getFilterNotGrouped());
        if (StringUtils.hasText(groupIdStr) && filterNotGrouped) {
            return responseDataList;
        } else if (StringUtils.hasText(groupIdStr)) {
            List<DeviceGroupMappingPO> mappingPOList = deviceGroupService.findAllMappingByGroupId(Long.valueOf(searchDeviceRequest.getGroupId()));
            if (mappingPOList.isEmpty()) {
                return responseDataList;
            }

            Set<Long> deviceIdSet = new HashSet<>();
            mappingPOList.forEach(mappingPO -> deviceIdSet.add(mappingPO.getDeviceId()));
            filter = filter.andThen(f -> f.in(DevicePO.Fields.id, deviceIdSet.toArray()));
        } else if (filterNotGrouped) {
            Long[] groupedDeviceIds = deviceGroupService.findAllGroupedDeviceIdList().toArray(Long[]::new);
            if (groupedDeviceIds.length > 0) {
                filter = filter.andThen(f -> f.notIn(DevicePO.Fields.id, groupedDeviceIds));
            }
        }

        try {
            responseDataList = deviceRepository
                    .findAllWithDataPermission(filter, searchDeviceRequest.toPageable())
                    .map(this::convertPOToResponseData);
        } catch (Exception e) {
            if (e instanceof ServiceException serviceException
                    && ErrorCode.FORBIDDEN_PERMISSION.getErrorCode().equals(serviceException.getErrorCode())) {
                return Page.empty();
            }
            throw e;
        }

        fillRelativeInfo(responseDataList.getContent());
        return responseDataList;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDevice(Long deviceId, UpdateDeviceRequest updateDeviceRequest) {
        Optional<DevicePO> findResult = deviceRepository.findByIdWithDataPermission(deviceId);
        if (findResult.isEmpty()) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).build();
        }

        DevicePO device = findResult.get();
        String newName = updateDeviceRequest.getName();
        if (device.getName().equals(newName)) {
            return;
        }

        device.setName(newName);

        deviceRepository.save(device);
        self.evictIntegrationIdToDeviceCache(device.getIntegration());
        eventBus.publish(DeviceEvent.of(DeviceEvent.EventType.UPDATED, deviceConverter.convertPO(device)));
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDeleteDevices(List<String> deviceIdList) {
        if (deviceIdList.isEmpty()) {
            return;
        }

        List<DevicePO> devicePOList = deviceRepository.findByIdInWithDataPermission(deviceIdList.stream().map(Long::valueOf).toList());
        Set<String> foundIds = devicePOList.stream().map(id -> id.getId().toString()).collect(Collectors.toSet());


        // check whether all devices exist
        if (!new HashSet<>(deviceIdList).containsAll(foundIds)) {
            throw ServiceException
                    .with(ErrorCode.DATA_NO_FOUND)
                    .detailMessage("Some id not found!")
                    .build();
        }

        List<Device> devices = deviceConverter.convertPO(devicePOList);

        Map<String, Integration> integrationMap = getIntegrationMap(devices.stream().map(Device::getIntegrationId).toList());

        devices.stream()
                .map((Device device) -> {
                    // check ability to delete device
                    Integration integrationConfig = integrationMap.get(device.getIntegrationId());
                    if (integrationConfig == null) {
                        self.deleteDevice(device);
                        return null;
                    }

                    ExchangePayload payload = new ExchangePayload();
                    String deleteDeviceServiceKey = integrationConfig.getEntityKeyDeleteDevice();
                    if (deleteDeviceServiceKey == null) {
                        throw ServiceException
                                .with(ErrorCode.METHOD_NOT_ALLOWED)
                                .detailMessage("integration " + device.getIntegrationId() + " cannot delete device!")
                                .build();
                    }

                    payload.put(deleteDeviceServiceKey, "");
                    payload.putContext(DEVICE_ON_DELETE, device);
                    return payload;
                })
                .filter(Objects::nonNull)
                .forEach((ExchangePayload payload) -> {
                    // call service for deleting
                    entityValueServiceProvider.saveValuesAndPublishSync(payload);
                });
    }

    public DeviceDetailResponse getDeviceDetail(Long deviceId) {
        Optional<DevicePO> findResult = deviceRepository.findByIdWithDataPermission(deviceId);
        if (findResult.isEmpty()) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).build();
        }

        DeviceDetailResponse deviceDetailResponse = new DeviceDetailResponse();

        // set detail data
        BeanUtils.copyProperties(convertPOToResponseData(findResult.get()), deviceDetailResponse);
        fillRelativeInfo(List.of(deviceDetailResponse));
        deviceDetailResponse.setIdentifier(findResult.get().getIdentifier());

        if (findResult.get().getUserId() != null) {
            List<UserDTO> userDTOList = userFacade.getUserByIds(List.of(findResult.get().getUserId()));
            if (!userDTOList.isEmpty()) {
                UserDTO user = userDTOList.get(0);
                deviceDetailResponse.setUserNickname(user.getNickname());
                deviceDetailResponse.setUserEmail(user.getEmail());
            }
        }

        if (findResult.get().getTemplate() != null) {
            List<DeviceTemplateDTO> deviceTemplateDTOList = deviceTemplateFacade.getDeviceTemplateByKeys(List.of(findResult.get().getTemplate()));
            if (!CollectionUtils.isEmpty(deviceTemplateDTOList)) {
                DeviceTemplateDTO deviceTemplate = deviceTemplateDTOList.get(0);
                deviceDetailResponse.setTemplateName(deviceTemplate.getName());
            }
        }

        // set entities
        List<Entity> entities = entityServiceProvider.findByTargetId(AttachTargetType.DEVICE, deviceId.toString());
        List<Long> entityIds = entities.stream().flatMap(entity -> Stream.concat(
                        Stream.of(entity.getId()),
                        Optional.ofNullable(entity.getChildren())
                                .map(e -> e.stream().map(Entity::getId))
                                .orElseGet(Stream::empty)))
                .toList();
        Map<Long, List<EntityTag>> entityIdToTags = entityServiceProvider.findTagsByIds(entityIds);
        deviceDetailResponse.setEntities(entities
                .stream().flatMap((Entity pEntity) -> {
                    List<Entity> flatEntities = new ArrayList<>();
                    flatEntities.add(pEntity);

                    List<Entity> childrenEntities = pEntity.getChildren();
                    if (childrenEntities != null) {
                        flatEntities.addAll(childrenEntities);
                    }

                    List<EntityTag> entityTags = entityIdToTags.get(pEntity.getId());

                    return flatEntities.stream().map(entity -> DeviceEntityData
                            .builder()
                            .id(entity.getId().toString())
                            .key(entity.getKey())
                            .type(entity.getType())
                            .name(entity.getName())
                            .valueType(entity.getValueType())
                            .valueAttribute(entity.getAttributes())
                            .description(entity.getDescription())
                            .entityTags(entityTags)
                            .build());
                }).toList());
        return deviceDetailResponse;
    }

    // Device API Implementations

    private List<DeviceNameDTO> convertDevicePOList(List<DevicePO> devicePOList) {
        List<Long> deviceIds = devicePOList.stream().map(DevicePO::getId).toList();
        Map<Long, List<DeviceGroupPO>> deviceIdToGroups = deviceGroupService.deviceIdToGroups(deviceIds);
        Map<String, Integration> integrationMap = getIntegrationMap(devicePOList.stream().map(DevicePO::getIntegration).toList());
        return devicePOList.stream()
                .map(devicePO -> DeviceNameDTO.builder()
                        .id(devicePO.getId())
                        .name(devicePO.getName())
                        .key(devicePO.getKey())
                        .userId(devicePO.getUserId())
                        .template(devicePO.getTemplate())
                        .createdAt(devicePO.getCreatedAt())
                        .integrationId(devicePO.getIntegration())
                        .integrationConfig(integrationMap.get(devicePO.getIntegration()))
                        .build())
                .peek(device -> {
                    List<DeviceGroupPO> deviceGroupPOList = deviceIdToGroups.get(device.getId());
                    if (!CollectionUtils.isEmpty(deviceGroupPOList)) {
                        device.setGroupId(deviceGroupPOList.get(0).getId());
                        device.setGroupName(deviceGroupPOList.get(0).getName());
                    }
                })
                .toList();
    }

    @Override
    public List<DeviceNameDTO> fuzzySearchDeviceByName(String name) {
        return convertDevicePOList(deviceRepository.findAll(f -> f.likeIgnoreCase(DevicePO.Fields.name, name)));
    }

    @Override
    public List<DeviceNameDTO> getDeviceNameByIntegrations(List<String> integrationIds) {
        return convertDevicePOList(self.mapIntegrationIdToDevices(integrationIds)
                .values()
                .stream()
                .flatMap(Collection::stream)
                .toList());
    }

    @BatchCacheable(cacheNames = CacheKeyConstants.INTEGRATION_ID_TO_DEVICE, keyPrefix = CacheKeyConstants.TENANT_PREFIX)
    public Map<String, List<DevicePO>> mapIntegrationIdToDevices(@CacheKeys List<String> integrationIds) {
        if (integrationIds == null || integrationIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return deviceRepository.findAll(f -> f.in(DevicePO.Fields.integration, integrationIds.toArray()))
                .stream()
                .collect(Collectors.groupingBy(DevicePO::getIntegration));
    }

    @Override
    public List<DeviceNameDTO> getDeviceNameByIds(List<Long> deviceIds) {
        if (deviceIds == null || deviceIds.isEmpty()) {
            return new ArrayList<>();
        }
        return convertDevicePOList(deviceRepository.findByIdIn(deviceIds));
    }

    @Override
    public List<DeviceNameDTO> getDeviceNameByKey(List<String> deviceKeys) {
        if (deviceKeys == null || deviceKeys.isEmpty()) {
            return new ArrayList<>();
        }
        return convertDevicePOList(deviceRepository.findAll(f -> f.in(DevicePO.Fields.key, deviceKeys.toArray())));
    }

    @Override
    public DeviceNameDTO getDeviceNameByKey(String deviceKey) {
        Optional<DevicePO> devicePO = deviceRepository.findOne(f -> f.eq(DevicePO.Fields.key, deviceKey));
        return devicePO.map(po -> convertDevicePOList(List.of(po)).get(0)).orElse(null);

    }

    @Override
    public Map<String, Long> countByIntegrationIds(List<String> integrationIds) {
        List<Object[]> res = deviceRepository.countByIntegrations(integrationIds);
        Map<String, Long> result = new HashMap<>();
        res.forEach((Object[] o) -> result.put((String) o[0], (Long) o[1]));
        return result;
    }

    @Override
    public Long countByIntegrationId(String integrationId) {
        return deviceRepository.count(f -> f.eq(DevicePO.Fields.integration, integrationId));
    }

    public void deleteDevice(Device device) {
        entityServiceProvider.deleteByTargetId(device.getId().toString());
        deviceGroupService.removeDevices(List.of(device.getId()));

        deviceRepository.deleteById(device.getId());
        self.evictIntegrationIdToDeviceCache(device.getIntegrationId());

        userFacade.deleteResource(ResourceType.DEVICE, Collections.singletonList(device.getId()));
        userFacade.deleteResource(ResourceType.ENTITY, device.getEntities().stream().map(Entity::getId).toList());

        eventBus.publish(DeviceEvent.of(DeviceEvent.EventType.DELETED, device));
    }

    @CacheEvict(cacheNames = CacheKeyConstants.INTEGRATION_ID_TO_DEVICE, key = "T(com.milesight.beaveriot.context.security.TenantContext).getTenantId()+':'+#p0")
    public void evictIntegrationIdToDeviceCache(String integrationId) {
        // do nothing
    }

    @BatchCacheEvict(cacheNames = CacheKeyConstants.INTEGRATION_ID_TO_DEVICE, keyPrefix = CacheKeyConstants.TENANT_PREFIX)
    public void evictIntegrationIdToDeviceCache(@CacheKeys Collection<String> integrationIds) {
        // do nothing
    }

    public void moveDeviceToGroup(MoveDeviceToGroupRequest request) {
        if (request.getDeviceIdList() == null || request.getDeviceIdList().isEmpty()) {
            return;
        }

        List<Long> deviceIdList = request.getDeviceIdList().stream().map(Long::valueOf).toList();

        // check permission
        deviceRepository.findByIdInWithDataPermission(deviceIdList);

        if (!StringUtils.hasText(request.getGroupId())) {
            deviceGroupService.removeDevices(deviceIdList);
        } else {
            Long groupId = Long.valueOf(request.getGroupId());
            deviceGroupService.getDeviceGroup(groupId);
            deviceGroupService.moveDevicesToGroupId(groupId, deviceIdList);
        }
    }
}
