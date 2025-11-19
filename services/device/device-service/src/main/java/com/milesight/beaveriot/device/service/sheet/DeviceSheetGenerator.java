package com.milesight.beaveriot.device.service.sheet;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import lombok.Getter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Consumer;

/**
 * DeviceSheetGenerator class.
 *
 * @author simon
 * @date 2025/6/26
 */
public class DeviceSheetGenerator {
    @Getter
    private final Set<String> columnNameSet = new HashSet<>();

    private XSSFWorkbook workbook;

    private XSSFSheet deviceSheet;

    private final Map<String, XSSFSheet> hiddenSheetMap = new HashMap<>();

    private int optUsage = 0;

    private DataValidationHelper deviceDataValidationHelper;

    public XSSFWorkbook getWorkbook() {
        if (workbook == null) {
            workbook = new XSSFWorkbook();
        }

        return workbook;
    }

    private XSSFSheet getDeviceSheet() {
        if (deviceSheet == null) {
            deviceSheet = getWorkbook().createSheet(DeviceSheetConstants.DEVICE_SHEET_NAME);

            // create header row
            deviceSheet.createRow(0);
            deviceSheet.createFreezePane(0, 1);
        }

        return deviceSheet;
    }

    private Row getDeviceSheetHeaderRow() {
        return getDeviceSheet().getRow(0);
    }

    private XSSFSheet getHiddenSheet(String sheetName, Consumer<XSSFSheet> createPreset) {
        return hiddenSheetMap.computeIfAbsent(sheetName, k -> {
            XSSFSheet hiddenSheet = getWorkbook().createSheet(sheetName);
            int sheetIdx = getWorkbook().getSheetIndex(hiddenSheet);
            getWorkbook().setSheetHidden(sheetIdx, true);
            if (createPreset != null) {
                createPreset.accept(hiddenSheet);
            }
            return hiddenSheet;
        });
    }

    private XSSFSheet getHiddenOptionSheet() {
        return getHiddenSheet(DeviceSheetConstants.HIDDEN_OPTION_SHEET, null);
    }

    private XSSFSheet getHiddenColMetaSheet() {
        return getHiddenSheet(DeviceSheetConstants.HIDDEN_COL_META_SHEET, sheet -> {
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue(DeviceSheetConstants.HIDDEN_COL_META_INDEX);
            row.createCell(1).setCellValue(DeviceSheetConstants.HIDDEN_COL_META_KEY);
            row.createCell(2).setCellValue(DeviceSheetConstants.HIDDEN_COL_META_NAME);
        });
    }

    private void applyDeviceInputConstraint(int col, DataValidationConstraint constraint) {
        CellRangeAddressList inputRange = new CellRangeAddressList(1, DeviceSheetConstants.MAX_BATCH_NUMBER, col, col);
        DataValidation validation = getDeviceDataValidationHelper().createValidation(constraint, inputRange);
        validation.setErrorStyle(DataValidation.ErrorStyle.STOP);
        validation.setShowErrorBox(true);
        getDeviceSheet().addValidationData(validation);
    }

    private DataValidationHelper getDeviceDataValidationHelper() {
        if (deviceDataValidationHelper == null) {
            deviceDataValidationHelper = new XSSFDataValidationHelper(getDeviceSheet());
        }

        return deviceDataValidationHelper;
    }

    private CellStyle generateDeviceHeaderStyle(DeviceSheetColumn column) {
        XSSFCellStyle style = getWorkbook().createCellStyle();

        // set text alignment
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        // set font
        XSSFFont font = getWorkbook().createFont();
        font.setBold(true);

        BorderStyle borderStyle = BorderStyle.MEDIUM;

        // set required style
        if (Boolean.TRUE.equals(column.getRequired())) {
            DataFormat dataFormat = getWorkbook().createDataFormat();
            style.setDataFormat(dataFormat.getFormat("\"*\"@"));
        }

        // set border
        style.setBorderBottom(borderStyle);
        style.setBorderLeft(borderStyle);
        style.setBorderRight(borderStyle);
        style.setBorderTop(borderStyle);

        style.setFont(font);
        return style;
    }

    private String getFormatStringFromFractionDigits(int fractionDigits) {
        StringBuilder sb = new StringBuilder();
        sb.append("0");
        if (fractionDigits > 0) {
            sb.append(".");
            sb.append("0".repeat(fractionDigits));
        }
        return sb.toString();
    }

    private void addColumnMeta(int colIndex, DeviceSheetColumn column) {
        int rowNum = getHiddenColMetaSheet().getLastRowNum() + 1;
        Row row = getHiddenColMetaSheet().createRow(rowNum);
        row.createCell(0).setCellValue(colIndex);
        row.createCell(1).setCellValue(column.getKey());
        row.createCell(2).setCellValue(column.getName());
    }

    public void addColumn(DeviceSheetColumn column) {
        if (getColumnNameSet().contains(column.getName())) {
            throw ServiceException
                    .with(ErrorCode.SERVER_ERROR.getErrorCode(), "duplicated column key: " + column.getName())
                    .build();
        }

        int columnIndex = getColumnNameSet().size();
        getColumnNameSet().add(column.getName());

        // build header row
        Row headerRow = getDeviceSheetHeaderRow();
        headerRow.setHeight(DeviceSheetConstants.HEADER_ROW_HEIGHT);
        Cell cell = headerRow.createCell(columnIndex);
        cell.setCellValue(column.getName());
        cell.setCellStyle(generateDeviceHeaderStyle(column));
        this.resolveInputConstraint(columnIndex, column);
        getDeviceSheet().autoSizeColumn(columnIndex);
        getDeviceSheet().setColumnWidth(columnIndex, getDeviceSheet().getColumnWidth(columnIndex) + DeviceSheetConstants.ADDITIONAL_COLUMN_WIDTH);

        // add column meta data
        this.addColumnMeta(columnIndex, column);
    }

    private String lengthCondition(String r, String op, Integer target) {
        return "LEN(%s)%s%d".formatted(r, op, target);
    }

    private String createOption(List<String> options) {
        for (int i = 0; i < options.size(); i++) {
            Row row = getHiddenOptionSheet().getRow(i);
            if (row == null) {
                row = getHiddenOptionSheet().createRow(i);
            }

            row.createCell(optUsage).setCellValue(options.get(i));
        }

        Name namedRange = getWorkbook().createName();
        String name = "opt_" + SnowflakeUtil.nextId();
        namedRange.setNameName(name);
        CellReference startRef = new CellReference(0, optUsage, true, true);
        CellReference endRef = new CellReference(Math.max(options.size() - 1, 0), optUsage, true, true);
        namedRange.setRefersToFormula(String.format("'%s'!%s:%s",
                getHiddenOptionSheet().getSheetName(),
                startRef.formatAsString(),
                endRef.formatAsString()
                )
        );
        optUsage += 1;
        return name;
    }

    private DataValidationConstraint createTextColumnConstraint(int columnIndex, DeviceSheetColumn column) {
        String colRef = new CellReference(1, columnIndex).formatAsString(false);

        // set default input style
        CellStyle textStyle = getWorkbook().createCellStyle();
        DataFormat format = getWorkbook().createDataFormat();
        textStyle.setDataFormat(format.getFormat("@"));
        textStyle.setQuotePrefixed(true);
        getDeviceSheet().setDefaultColumnStyle(columnIndex, textStyle);

        List<String> condList = new ArrayList<>();

        // max length
        if (column.getMaxLength() != null) {
            condList.add(lengthCondition(colRef, "<=", column.getMaxLength()));
        }

        // min length
        if (column.getMinLength() != null) {
            condList.add(lengthCondition(colRef, ">=", column.getMinLength()));
        }

        // length range
        if (StringUtils.hasText(column.getLengthRange())) {
            String[] rList = column.getLengthRange().split(",");
            List<String> rCondList = new ArrayList<>();
            for (String r : rList) {
                rCondList.add(lengthCondition(colRef, "=", Integer.valueOf(r)));
            }

            condList.add("OR(" + String.join(",", rCondList) + ")");
        }

        if (Boolean.TRUE.equals(column.getIsHexString())) {
            condList.add("SUMPRODUCT(--ISNUMBER(FIND(MID(UPPER(%s),ROW(INDIRECT(\"1:\"&LEN(%s))),1),\"0123456789ABCDEF\"))) = LEN(%s)".formatted(colRef, colRef, colRef));
        }

        if (condList.isEmpty()) {
            return null;
        }

        return getDeviceDataValidationHelper().createCustomConstraint("AND(" + String.join(",", condList) + ")");
    }

    private DataValidationConstraint createBooleanColumnConstraint() {
        return getDeviceDataValidationHelper().createExplicitListConstraint(new String[]{"TRUE", "FALSE"});
    }

    private DataValidationConstraint createEnumColumnConstraint(DeviceSheetColumn column) {
        List<String> enums = Optional
                .ofNullable(column.getEnums())
                .orElseGet(Map::of)
                .keySet()
                .stream()
                .filter(StringUtils::hasText)
                .toList();
        return getDeviceDataValidationHelper().createFormulaListConstraint(createOption(enums));
    }

    private DataValidationConstraint createLongColumnConstraint(DeviceSheetColumn column) {
        if (column.getMax() != null && column.getMin() != null) {
            return getDeviceDataValidationHelper().createIntegerConstraint(
                    DataValidationConstraint.OperatorType.BETWEEN,
                    String.valueOf(column.getMin().longValue()),
                    String.valueOf(column.getMax().longValue())
            );
        } else if (column.getMax() != null) {
            return getDeviceDataValidationHelper().createIntegerConstraint(
                    DataValidationConstraint.OperatorType.LESS_OR_EQUAL,
                    String.valueOf(column.getMax().longValue()),
                    null
            );
        } else if (column.getMin() != null) {
            return getDeviceDataValidationHelper().createIntegerConstraint(
                    DataValidationConstraint.OperatorType.GREATER_OR_EQUAL,
                    String.valueOf(column.getMin().longValue()),
                    null
            );
        }

        return null;
    }

    private DataValidationConstraint createDoubleColumnConstraint(int columnIndex, DeviceSheetColumn column) {
        Integer fractionDigits = column.getFractionDigits();
        if (fractionDigits != null && fractionDigits >= 0) {
            CellStyle cellStyle = getWorkbook().createCellStyle();
            DataFormat format = getWorkbook().createDataFormat();
            cellStyle.setDataFormat(format.getFormat(getFormatStringFromFractionDigits(fractionDigits)));
            getDeviceSheet().setDefaultColumnStyle(columnIndex, cellStyle);
        }

        if (column.getMax() != null && column.getMin() != null) {
            return getDeviceDataValidationHelper().createDecimalConstraint(
                    DataValidationConstraint.OperatorType.BETWEEN,
                    String.valueOf(column.getMin()),
                    String.valueOf(column.getMax())
            );
        } else if (column.getMax() != null) {
            return getDeviceDataValidationHelper().createDecimalConstraint(
                    DataValidationConstraint.OperatorType.LESS_OR_EQUAL,
                    String.valueOf(column.getMax()),
                    null
            );
        } else if (column.getMin() != null) {
            return getDeviceDataValidationHelper().createDecimalConstraint(
                    DataValidationConstraint.OperatorType.GREATER_OR_EQUAL,
                    String.valueOf(column.getMin()),
                    null
            );
        }

        return null;
    }

    private void resolveInputConstraint(int columnIndex, DeviceSheetColumn column) {
        DataValidationConstraint constraint;
        switch (column.getType()) {
            case DeviceSheetColumn.COLUMN_TYPE_TEXT -> constraint = createTextColumnConstraint(columnIndex, column);
            case DeviceSheetColumn.COLUMN_TYPE_BOOLEAN -> constraint = createBooleanColumnConstraint();
            case DeviceSheetColumn.COLUMN_TYPE_ENUM -> constraint = createEnumColumnConstraint(column);
            case DeviceSheetColumn.COLUMN_TYPE_LONG -> constraint = createLongColumnConstraint(column);
            case DeviceSheetColumn.COLUMN_TYPE_DOUBLE -> constraint = createDoubleColumnConstraint(columnIndex, column);
            default -> throw ServiceException.with(ErrorCode.SERVER_ERROR.getErrorCode(), "Unknown column type: " + column.getType()).build();
        }

        if (constraint != null) {
            this.applyDeviceInputConstraint(columnIndex, constraint);
        }
    }
}
