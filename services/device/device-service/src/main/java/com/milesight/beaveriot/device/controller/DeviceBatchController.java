package com.milesight.beaveriot.device.controller;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.response.ResponseBody;
import com.milesight.beaveriot.base.response.ResponseBuilder;
import com.milesight.beaveriot.device.enums.DeviceErrorCode;
import com.milesight.beaveriot.device.model.request.BatchDeviceErrorRequest;
import com.milesight.beaveriot.device.model.request.BatchDeviceParseRequest;
import com.milesight.beaveriot.device.model.request.BatchDeviceTemplateRequest;
import com.milesight.beaveriot.device.model.response.DeviceListSheetParseResponse;
import com.milesight.beaveriot.device.service.DeviceBatchService;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DeviceBatchController class.
 *
 * @author simon
 * @date 2025/6/26
 */
@RestController
@RequestMapping("/device-batch")
@Slf4j
public class DeviceBatchController {
    @Autowired
    DeviceBatchService deviceBatchService;

    private String getFilePrefix() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_M_d_H_m_s");
        return now.format(formatter);
    }

    private HttpHeaders getFileHeaders(String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);
        headers.add(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); // From https://en.wikipedia.org/wiki/Media_type#Common_examples
        return headers;
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_ADD)
    @PostMapping("/template")
    public ResponseEntity<byte[]> generateTemplate(@RequestBody @Valid BatchDeviceTemplateRequest request) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (Workbook workbook = deviceBatchService.generateTemplate(request.getIntegration())) {
            workbook.write(bos);
        } catch (IOException e) {
            log.error(e.toString());
            throw ServiceException
                    .with(ErrorCode.SERVER_ERROR.getErrorCode(), "Building batch device template failed in IO!")
                    .build();
        }

        return ResponseEntity.ok()
                .headers(getFileHeaders(getFilePrefix() + "_devices_import_template.xlsx"))
                .body(bos.toByteArray());
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_ADD)
    @PostMapping("/parse")
    public ResponseBody<DeviceListSheetParseResponse> parseBatchFile(@ModelAttribute @Valid BatchDeviceParseRequest request) {
        try (Workbook workbook = WorkbookFactory.create(request.getFile().getInputStream())) {
            return ResponseBuilder.success(deviceBatchService.parseTemplate(request.getIntegration(), workbook));
        } catch (IOException e) {
            throw ServiceException
                    .with(ErrorCode.SERVER_ERROR.getErrorCode(), "Parsing device list failed in IO!")
                    .build();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw ServiceException
                    .with(DeviceErrorCode.DEVICE_LIST_SHEET_PARSING_ERROR)
                    .build();
        }
    }

    @OperationPermission(codes = OperationPermissionCode.DEVICE_ADD)
    @PostMapping("/fill-error")
    public ResponseEntity<byte[]> fillDeviceBatchError(@ModelAttribute @Valid BatchDeviceErrorRequest request) {
        try (Workbook workbook = WorkbookFactory.create(request.getFile().getInputStream())) {
            deviceBatchService.fillError(workbook, request.getErrors(), request.getIntegration());
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            workbook.write(bos);
            return ResponseEntity.ok()
                    .headers(getFileHeaders(getFilePrefix() + "_error_messages.xlsx"))
                    .body(bos.toByteArray());
        } catch (IOException e) {
            throw ServiceException
                    .with(ErrorCode.SERVER_ERROR.getErrorCode(), "Fill device error failed in IO!")
                    .build();
        }
    }
}
