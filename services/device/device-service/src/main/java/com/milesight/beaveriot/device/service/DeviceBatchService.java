package com.milesight.beaveriot.device.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.api.EntityServiceProvider;
import com.milesight.beaveriot.context.api.IntegrationServiceProvider;
import com.milesight.beaveriot.context.integration.enums.EntityValueType;
import com.milesight.beaveriot.context.integration.model.AttributeBuilder;
import com.milesight.beaveriot.context.integration.model.AttributeFormatComponent;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.Integration;
import com.milesight.beaveriot.device.constants.DeviceDataFieldConstants;
import com.milesight.beaveriot.device.service.sheet.DeviceSheetColumn;
import com.milesight.beaveriot.device.service.sheet.DeviceSheetConstants;
import com.milesight.beaveriot.device.service.sheet.DeviceSheetGenerator;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    public byte[] generateTemplate(String integrationId) {
        List<Entity> entities = getDeviceAddingEntities(integrationId);
        try (DeviceSheetGenerator deviceSheetGenerator = new DeviceSheetGenerator()) {
            DeviceSheetColumn nameColumn = new DeviceSheetColumn();
            nameColumn.setName(DeviceSheetConstants.DEVICE_NAME_COL_NAME);
            nameColumn.setType(DeviceSheetColumn.COLUMN_TYPE_TEXT);
            nameColumn.setMinLength(1);
            nameColumn.setMaxLength(DeviceDataFieldConstants.DEVICE_NAME_MAX_LENGTH);
            nameColumn.setKey(DeviceSheetConstants.DEVICE_NAME_COL_KEY);
            deviceSheetGenerator.addColumn(nameColumn);

            entities.forEach(entity -> {
                DeviceSheetColumn entityColumn = new DeviceSheetColumn();
                entityColumn.setName(entity.getName());
                entityColumn.setKey(entity.getKey());
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
                    entityColumn.setEnums(enums.values().stream().toList());
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

                deviceSheetGenerator.addColumn(entityColumn);
            });
            return deviceSheetGenerator.output();
        } catch (IOException e) {
            throw ServiceException
                    .with(ErrorCode.SERVER_ERROR.getErrorCode(), "Build batch device template error in io!")
                    .build();
        }
    }

    private void parseTemplate(ByteArrayInputStream byteInputStream) {
        try (Workbook workbook = WorkbookFactory.create(byteInputStream)) {
        } catch (IOException e) {
            throw ServiceException
                    .with(ErrorCode.SERVER_ERROR.getErrorCode(), "Build batch device template error in io!")
                    .build();
        }
    }

    private Integration getIntegrationFromId(String integrationId) {
        Integration integration = integrationServiceProvider.getIntegration(integrationId);
        if (integration == null) {
            throw ServiceException
                    .with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Integration [" + integrationId + "] not exsists!")
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
}
