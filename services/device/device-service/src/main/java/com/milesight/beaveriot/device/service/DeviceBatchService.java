package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.integration.model.AttributeFormatComponent;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.device.constants.DeviceDataFieldConstants;
import com.milesight.beaveriot.device.model.DeviceBatchError;
import com.milesight.beaveriot.device.model.response.DeviceListSheetParseResponse;
import com.milesight.beaveriot.device.service.sheet.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * DeviceBatchService class.
 *
 * @author simon
 * @date 2025/6/26
 */
@Service
@Slf4j
public class DeviceBatchService {

    @Autowired
    IntegrationServiceProvider integrationServiceProvider;

    @Autowired
    EntityServiceProvider entityServiceProvider;

    public Workbook generateTemplate(String integrationId) {
        List<DeviceSheetColumn> columns = getSheetColumnsByEntities(getDeviceAddingEntities(integrationId));
        DeviceSheetGenerator deviceSheetGenerator = new DeviceSheetGenerator();
        columns.forEach(deviceSheetGenerator::addColumn);
        return deviceSheetGenerator.getWorkbook();
    }

    public DeviceListSheetParseResponse parseTemplate(String integrationId, Workbook workbook) {
        List<Entity> entities = getDeviceAddingEntities(integrationId);
        List<DeviceSheetColumn> columns = getSheetColumnsByEntities(entities);
        DeviceSheetParser parser = new DeviceSheetParser(workbook, columns);
        parser.validate();
        return parser.generateCreateRequest(entities, integrationId);
    }

    public void fillError(Workbook workbook, String errorData, String integrationId) {
        List<Entity> entities = getDeviceAddingEntities(integrationId);
        List<DeviceSheetColumn> columns = getSheetColumnsByEntities(entities);
        DeviceSheetErrorApplier processor = new DeviceSheetErrorApplier(workbook, columns.size());
        DeviceBatchError deviceBatchError = JsonUtils.fromJSON(errorData, DeviceBatchError.class);
        processor.apply(deviceBatchError);
    }

    private Integration getIntegrationFromId(String integrationId) {
        Integration integration = integrationServiceProvider.getIntegration(integrationId);
        if (integration == null) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Integration [" + integrationId + "] not exists!")
                    .build();
        }

        return integration;
    }

    private List<Entity> getDeviceAddingEntities(String integrationId) {
        Integration integration = getIntegrationFromId(integrationId);
        String addDeviceEntityKey = integration.getEntityKeyAddDevice();
        if (addDeviceEntityKey == null) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Integration [" + integrationId + "] does not support adding devices.")
                    .build();
        }

        return entityServiceProvider.findByKeys(List.of(addDeviceEntityKey)).get(addDeviceEntityKey).getChildren();
    }

    private List<DeviceSheetColumn> getSheetColumnsByEntities(List<Entity> entities) {
        List<DeviceSheetColumn> columns = new ArrayList<>();

        // add name column
        DeviceSheetColumn nameColumn = new DeviceSheetColumn();
        nameColumn.setName(DeviceSheetConstants.DEVICE_NAME_COL_NAME);
        nameColumn.setType(DeviceSheetColumn.COLUMN_TYPE_TEXT);
        nameColumn.setMinLength(1);
        nameColumn.setMaxLength(DeviceDataFieldConstants.DEVICE_NAME_MAX_LENGTH);
        nameColumn.setKey(DeviceSheetConstants.DEVICE_NAME_COL_KEY);
        columns.add(nameColumn);

        // add entities columns
        entities.forEach(entity -> {
            DeviceSheetColumn entityColumn = new DeviceSheetColumn();
            entityColumn.setName(entity.getName());
            entityColumn.setKey(entity.getKey());
            entityColumn.setRequired(!entity.isOptional());
            Map<String, Object> attributes = entity.getAttributes();

            Map<String, String> enums = null;
            String format = null;
            if (attributes != null) {
                enums = (Map<String, String>) attributes.get(AttributeBuilder.ATTRIBUTE_ENUM);
                format = (String) attributes.get(AttributeBuilder.ATTRIBUTE_FORMAT);

                // set attribute to column
                if (attributes.get(AttributeBuilder.ATTRIBUTE_MIN) != null) {
                    String min = attributes.get(AttributeBuilder.ATTRIBUTE_MIN).toString();
                    entityColumn.setMin(Double.valueOf(min));
                }
                if (attributes.get(AttributeBuilder.ATTRIBUTE_MAX) != null) {
                    String max = attributes.get(AttributeBuilder.ATTRIBUTE_MAX).toString();
                    entityColumn.setMax(Double.valueOf(max));
                }
                if (attributes.get(AttributeBuilder.ATTRIBUTE_MIN_LENGTH) != null) {
                    String minLength = attributes.get(AttributeBuilder.ATTRIBUTE_MIN_LENGTH).toString();
                    entityColumn.setMinLength(Integer.valueOf(minLength));
                }
                if (attributes.get(AttributeBuilder.ATTRIBUTE_MAX_LENGTH) != null) {
                    String maxLength = attributes.get(AttributeBuilder.ATTRIBUTE_MAX_LENGTH).toString();
                    entityColumn.setMaxLength(Integer.valueOf(maxLength));
                }
                if (attributes.get(AttributeBuilder.ATTRIBUTE_LENGTH_RANGE) != null) {
                    String lengthRange = attributes.get(AttributeBuilder.ATTRIBUTE_LENGTH_RANGE).toString();
                    entityColumn.setLengthRange(lengthRange);
                }
            }

            if (entity.getValueType().equals(EntityValueType.BOOLEAN)) {
                entityColumn.setType(DeviceSheetColumn.COLUMN_TYPE_BOOLEAN);
            } else if (enums != null) {
                entityColumn.setType(DeviceSheetColumn.COLUMN_TYPE_ENUM);
                entityColumn.setEnums(enums.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey, (k1, k2) -> k1, LinkedHashMap::new)));
            } else if (entity.getValueType().equals(EntityValueType.LONG)) {
                entityColumn.setType(DeviceSheetColumn.COLUMN_TYPE_LONG);
            } else if (entity.getValueType().equals(EntityValueType.DOUBLE)) {
                entityColumn.setType(DeviceSheetColumn.COLUMN_TYPE_DOUBLE);
            } else {
                if (format != null
                        && (
                        format.equals(AttributeFormatComponent.HEX.name())
                                ||
                                format.startsWith(AttributeFormatComponent.HEX.name() + ":")
                )
                ) {
                    entityColumn.setIsHexString(true);
                }
                entityColumn.setType(DeviceSheetColumn.COLUMN_TYPE_TEXT);
            }

            columns.add(entityColumn);
        });

        // add group column
        DeviceSheetColumn groupColumn = new DeviceSheetColumn();
        groupColumn.setName(DeviceSheetConstants.DEVICE_GROUP_COL_NAME);
        groupColumn.setType(DeviceSheetColumn.COLUMN_TYPE_TEXT);
        groupColumn.setMinLength(1);
        groupColumn.setMaxLength(DeviceDataFieldConstants.DEVICE_GROUP_NAME_MAX_LENGTH);
        groupColumn.setKey(DeviceSheetConstants.DEVICE_GROUP_COL_KEY);
        groupColumn.setRequired(false);
        columns.add(groupColumn);

        // add location related column
        DeviceSheetColumn latitudeColumn = new DeviceSheetColumn();
        latitudeColumn.setName(DeviceSheetConstants.DEVICE_LOCATION_LATITUDE_COL_NAME);
        latitudeColumn.setType(DeviceSheetColumn.COLUMN_TYPE_DOUBLE);
        latitudeColumn.setKey(DeviceSheetConstants.DEVICE_LOCATION_LATITUDE_COL_KEY);
        latitudeColumn.setMin(-90.0);
        latitudeColumn.setMax(90.0);
        latitudeColumn.setFractionDigits(6);
        latitudeColumn.setRequired(false);
        columns.add(latitudeColumn);

        DeviceSheetColumn longitudeColumn = new DeviceSheetColumn();
        longitudeColumn.setName(DeviceSheetConstants.DEVICE_LOCATION_LONGITUDE_COL_NAME);
        longitudeColumn.setType(DeviceSheetColumn.COLUMN_TYPE_DOUBLE);
        longitudeColumn.setKey(DeviceSheetConstants.DEVICE_LOCATION_LONGITUDE_COL_KEY);
        longitudeColumn.setMin(-180.0);
        longitudeColumn.setMax(180.0);
        longitudeColumn.setFractionDigits(6);
        longitudeColumn.setRequired(false);
        columns.add(longitudeColumn);

        DeviceSheetColumn addressColumn = new DeviceSheetColumn();
        addressColumn.setName(DeviceSheetConstants.DEVICE_LOCATION_ADDRESS_COL_NAME);
        addressColumn.setType(DeviceSheetColumn.COLUMN_TYPE_TEXT);
        addressColumn.setKey(DeviceSheetConstants.DEVICE_LOCATION_ADDRESS_COL_KEY);
        addressColumn.setMinLength(1);
        addressColumn.setMaxLength(DeviceDataFieldConstants.DEVICE_ADDRESS_MAX_LENGTH);
        addressColumn.setRequired(false);
        columns.add(addressColumn);

        return columns;
    }
}
