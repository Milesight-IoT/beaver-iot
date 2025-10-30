package com.milesight.beaveriot.device.service.sheet;

import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.integration.model.Entity;
import com.milesight.beaveriot.context.integration.model.ExchangePayload;
import com.milesight.beaveriot.device.enums.DeviceErrorCode;
import com.milesight.beaveriot.device.model.request.CreateDeviceRequest;
import com.milesight.beaveriot.device.model.response.DeviceListSheetParseResponse;
import lombok.Data;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DeviceSheetParser class.
 *
 * @author simon
 * @date 2025/7/4
 */
public class DeviceSheetParser {
    private final Workbook workbook;

    private final List<DeviceSheetColumn> deviceListColumns;

    @Data
    private static class ColumnMetaData {
        private Integer colIndex;

        private String key;

        private String name;
    }

    private final String[] columnMetaHeaders = {
            DeviceSheetConstants.HIDDEN_COL_META_INDEX,
            DeviceSheetConstants.HIDDEN_COL_META_KEY,
            DeviceSheetConstants.HIDDEN_COL_META_NAME,
    };

    private List<ColumnMetaData> columnMetaList = null;

    public DeviceSheetParser(Workbook workbook, List<DeviceSheetColumn> deviceListColumns) {
        this.workbook = workbook;
        this.deviceListColumns = deviceListColumns;
    }

    public void validate() {
        if (!doValidateStructure()) {
            throw ServiceException.with(DeviceErrorCode.DEVICE_LIST_SHEET_STRUCTURE_ERROR).build();
        }

        if (getDeviceListSheet().getLastRowNum() < 1) {
            throw ServiceException.with(DeviceErrorCode.DEVICE_LIST_SHEET_NO_DEVICE).build();
        }
    }

    private Sheet getDeviceListSheet() {
        return workbook.getSheet(DeviceSheetConstants.DEVICE_SHEET_NAME);
    }

    private Sheet getColumnMetaSheet() {
        return workbook.getSheet(DeviceSheetConstants.HIDDEN_COL_META_SHEET);
    }

    private String getCellValue(Cell cell) {
        String cellStr = "";
        if (cell == null) {
            return cellStr;
        }

        cellStr = switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula(); // 或者用 formatter.formatCellValue(cell)
            default -> "";
        };

        return cellStr;
    }

    private List<ColumnMetaData> getColumnMetaList() {
        if (this.columnMetaList != null) {
            return this.columnMetaList;
        }

        List<ColumnMetaData> result = new ArrayList<>();
        Sheet columnMetaSheet = getColumnMetaSheet();
        for (int i = 1; i <= columnMetaSheet.getLastRowNum(); i++) {
            Row row = columnMetaSheet.getRow(i);
            ColumnMetaData data = new ColumnMetaData();
            data.setColIndex(Double.valueOf(getCellValue(row.getCell(0))).intValue());
            data.setKey(getCellValue(row.getCell(1)));
            data.setName(getCellValue(row.getCell(2)));
            result.add(data);
        }

        this.columnMetaList = result;
        return this.columnMetaList;
    }

    private boolean doValidateStructure() {
        if (Arrays.stream(DeviceSheetConstants.REQUIRED_SHEETS).anyMatch(sheetName -> workbook.getSheet(sheetName) == null)) {
            return false;
        }

        // validate column meta
        Sheet columnMetaSheet = getColumnMetaSheet();
        Row metaHeaderRow = columnMetaSheet.getRow(0);
        if (metaHeaderRow == null) {
            return false;
        }

        for (int i = 0; i < columnMetaHeaders.length; i++) {
            Cell cell = metaHeaderRow.getCell(i);
            if (cell == null || !columnMetaHeaders[i].equals(cell.getStringCellValue())) {
                return false;
            }
        }

        if (columnMetaSheet.getLastRowNum() != this.deviceListColumns.size()) {
            return false;
        }

        Map<String, String> columnKeyToName = new HashMap<>();
        this.deviceListColumns.forEach(deviceListColumn -> columnKeyToName.put(deviceListColumn.getKey(), deviceListColumn.getName()));

        Map<String, String> columnIndexToName = new HashMap<>();
        for (ColumnMetaData columnMetaData : this.getColumnMetaList()) {
            if (!columnKeyToName.get(columnMetaData.getKey()).equals(columnMetaData.getName())) {
                return false;
            }

            columnIndexToName.put(columnMetaData.getColIndex().toString(), columnMetaData.getName());
        }

        // validate if device columns match meta
        Sheet deviceListSheet = getDeviceListSheet();
        Row deviceListHeaderRow = deviceListSheet.getRow(0);
        if (deviceListHeaderRow == null) {
            return false;
        }

        if (this.getColumnMetaList().size() != deviceListHeaderRow.getLastCellNum()) {
            return false;
        }

        for (int i = 0; i < this.getColumnMetaList().size(); i++) {
            if (!getCellValue(deviceListHeaderRow.getCell(i)).equals(columnIndexToName.get(String.valueOf(i)))) {
                return false;
            }
        }

        return true;
    }

    public DeviceListSheetParseResponse generateCreateRequest(List<Entity> entities, String integrationId) {
        validate();
        Map<String, Entity> entityMap = entities.stream().collect(Collectors.toMap(Entity::getKey, Function.identity()));
        DeviceListSheetParseResponse result = new DeviceListSheetParseResponse();
        List<CreateDeviceRequest> createDeviceRequests = new ArrayList<>();
        List<Integer> rowIdList = new ArrayList<>();
        Map<String, DeviceSheetColumn> columnKeyMapping = this.deviceListColumns.stream().collect(Collectors.toMap(DeviceSheetColumn::getKey, Function.identity()));

        for (int i = 1; i <= this.getDeviceListSheet().getLastRowNum(); i++) {
            CreateDeviceRequest createDeviceRequest = new CreateDeviceRequest();
            createDeviceRequest.setIntegration(integrationId);
            Row row = this.getDeviceListSheet().getRow(i);
            boolean rowHasValue = false;
            for (ColumnMetaData columnMetaData : this.getColumnMetaList()) {
                Cell cell = row.getCell(columnMetaData.getColIndex());
                switch (columnMetaData.getKey()) {
                    case DeviceSheetConstants.DEVICE_NAME_COL_KEY -> {
                        String deviceName = getCellValue(cell);
                        if (StringUtils.hasText(deviceName)) {
                            createDeviceRequest.setName(deviceName);
                            rowHasValue = true;
                        }
                        continue;
                    }
                    case DeviceSheetConstants.DEVICE_GROUP_COL_KEY -> {
                        String groupName = getCellValue(cell).trim();
                        if (StringUtils.hasText(groupName)) {
                            createDeviceRequest.setGroupName(groupName);
                            rowHasValue = true;
                        }
                        continue;
                    }
                    case DeviceSheetConstants.DEVICE_LOCATION_LONGITUDE_COL_KEY -> {
                        String longitude = getCellValue(cell);
                        if (StringUtils.hasText(longitude)) {
                            createDeviceRequest.getOrCreateLocation().setLongitude(longitude);
                            rowHasValue = true;
                        }
                        continue;
                    }
                    case DeviceSheetConstants.DEVICE_LOCATION_LATITUDE_COL_KEY -> {
                        String latitude = getCellValue(cell);
                        if (StringUtils.hasText(latitude)) {
                            createDeviceRequest.getOrCreateLocation().setLatitude(latitude);
                            rowHasValue = true;
                        }
                        continue;
                    }
                    case DeviceSheetConstants.DEVICE_LOCATION_ADDRESS_COL_KEY -> {
                        String address = getCellValue(cell).trim();
                        if (StringUtils.hasText(address)) {
                            createDeviceRequest.getOrCreateLocation().setAddress(address);
                            rowHasValue = true;
                        }
                        continue;
                    }
                }

                if (createDeviceRequest.getParamEntities() == null) {
                    createDeviceRequest.setParamEntities(new ExchangePayload());
                }

                Entity entity = entityMap.get(columnMetaData.getKey());
                String strValue = getCellValue(cell);
                if (strValue == null || strValue.isEmpty()) {
                    continue;
                }

                Object value = strValue;
                DeviceSheetColumn column = columnKeyMapping.get(columnMetaData.getKey());
                if (column.getEnums() != null) {
                    value = column.getEnums().get(strValue);
                }

                if (value == null) {
                    continue;
                }

                try {
                    createDeviceRequest.getParamEntities().put(columnMetaData.getKey(), entity.getValueType().convertValue(value));
                } catch (IllegalArgumentException e) {
                    throw ServiceException.with(DeviceErrorCode.DEVICE_LIST_SHEET_PARSING_VALUE_ERROR).args(Map.of(
                            "expected_type", entity.getValueType(),
                            "entity_key", entity.getKey(),
                            "entity_name", entity.getName()
                    )).build();
                }

                rowHasValue = true;
            }

            if (rowHasValue) {
                createDeviceRequests.add(createDeviceRequest);
                rowIdList.add(row.getRowNum() - 1);

                if (createDeviceRequests.size() > DeviceSheetConstants.MAX_BATCH_NUMBER) {
                    throw ServiceException
                            .with(DeviceErrorCode.DEVICE_LIST_SHEET_DEVICE_OVER_LIMITATION)
                            .args(Map.of(
                                    "limitation", DeviceSheetConstants.MAX_BATCH_NUMBER,
                                    "detected", getDeviceListSheet().getLastRowNum()
                            ))
                            .build();
                }
            }
        }

        if (createDeviceRequests.isEmpty()) {
            throw ServiceException.with(DeviceErrorCode.DEVICE_LIST_SHEET_NO_DEVICE).build();
        }

        result.setCreateDeviceRequests(createDeviceRequests);
        result.setRowId(rowIdList);
        return result;
    }
}
