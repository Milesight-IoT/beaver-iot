package com.milesight.beaveriot.device.service.sheet;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.device.enums.DeviceErrorCode;
import com.milesight.beaveriot.device.model.DeviceBatchError;
import org.apache.poi.ss.usermodel.*;

import java.util.*;

/**
 * DeviceSheetErrorApplier class.
 *
 * @author simon
 * @date 2025/7/8
 */
public class DeviceSheetErrorApplier {

    private final Workbook workbook;

    private final int errorColIndex;

    private Sheet getDeviceListSheet() {
        Sheet deviceListSheet = workbook.getSheet(DeviceSheetConstants.DEVICE_SHEET_NAME);
        if (deviceListSheet == null) {
            throw ServiceException
                    .with(DeviceErrorCode.DEVICE_LIST_SHEET_PARSING_ERROR)
                    .build();
        }

        return deviceListSheet;
    }

    public DeviceSheetErrorApplier(Workbook workbook, int errorColIndex) {
        this.workbook = workbook;

        Row headerRow = getDeviceListSheet().getRow(0);
        this.errorColIndex = errorColIndex;
        Cell cell = headerRow.createCell(errorColIndex, CellType.STRING);
        cell.setCellValue(DeviceSheetConstants.DEVICE_SHEET_ERROR_COL_NAME);

        Font font = workbook.createFont();
        font.setColor(IndexedColors.RED.getIndex());

        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        cellStyle.setFont(font);

        cell.setCellStyle(cellStyle);
    }

    public void apply(DeviceBatchError deviceBatchError) {
        Sheet deviceListSheet = getDeviceListSheet();
        Set<Integer> keepRows = new LinkedHashSet<>(List.of(0));
        if (deviceBatchError.getErrors() == null || deviceBatchError.getErrors().isEmpty()) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "No errors.").build();
        }

        for (DeviceBatchError.ErrorDetail detail : deviceBatchError.getErrors()) {
            int rowIndex = detail.getId() + 1; // jump the header row
            Row row = deviceListSheet.getRow(rowIndex);
            if (row == null) {
                throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED.getErrorCode(), "Missed row: " + detail.getId()).build();
            }

            Cell cell = row.createCell(errorColIndex);
            CellStyle cellStyle = workbook.createCellStyle();
            cellStyle.setWrapText(true);
            cell.setCellStyle(cellStyle);
            cell.setCellValue(detail.getMsg());

            keepRows.add(rowIndex);
        }

        List<Integer> allRows = new ArrayList<>();
        for (Row row : deviceListSheet) {
            allRows.add(row.getRowNum());
        }

        // remove rows backward
        allRows.sort(Collections.reverseOrder());
        for (int rowNum : allRows) {
            if (!keepRows.contains(rowNum)) {
                deviceListSheet.removeRow(deviceListSheet.getRow(rowNum));
            }
        }

        // shrink the rows
        int newRowNum = 0;
        for (int oldRowNum : keepRows) {
            Row oldRow = deviceListSheet.getRow(oldRowNum);
            if (oldRow != null && oldRow.getRowNum() != newRowNum) {
                deviceListSheet.shiftRows(oldRowNum, oldRowNum, newRowNum - oldRowNum);
            }
            newRowNum++;
        }
    }
}
