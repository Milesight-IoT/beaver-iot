package com.milesight.beaveriot.entity.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.page.GenericPageRequest;
import com.milesight.beaveriot.base.page.Sorts;
import com.milesight.beaveriot.entity.dto.EntityResponse;
import com.milesight.beaveriot.entity.exporter.CsvExporter;
import com.milesight.beaveriot.entity.model.dto.EntityExportData;
import com.milesight.beaveriot.entity.model.request.EntityExportRequest;
import com.milesight.beaveriot.entity.model.request.EntityHistoryQuery;
import com.milesight.beaveriot.entity.model.response.EntityHistoryResponse;
import com.milesight.beaveriot.entity.po.EntityHistoryPO;
import com.milesight.beaveriot.entity.po.EntityPO;
import com.milesight.beaveriot.permission.aspect.OperationPermission;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.*;
import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Export entity data
 */
@Slf4j
@Service
public class EntityExportService {

    public static final DateTimeFormatter DEFAULT_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");

    private static final CsvExporter<EntityExportData> exporter = CsvExporter.newInstance(EntityExportData.class);

    @Autowired
    private EntityService entityService;

    @Autowired
    private EntityValueService entityValueService;

    private static String getDateTime(EntityHistoryResponse historyResponse, ZoneId zoneId) {
        val milliseconds = Long.parseLong(historyResponse.getTimestamp());
        val epochSecond = TimeUnit.MILLISECONDS.toSeconds(milliseconds);
        val instant = Instant.ofEpochSecond(epochSecond);
        val zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId);
        return zonedDateTime.format(DEFAULT_DATETIME_FORMATTER);
    }

    public void export(EntityExportRequest entityExportRequest, HttpServletResponse httpServletResponse) throws IOException {

        log.info("Export entity data: {}", entityExportRequest);

        val entityResponses = entityService.findEntityResponsesAndTheirChildrenByIds(entityExportRequest.getIds());
        if (entityResponses.isEmpty()) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND).detailMessage("entity not found").build();
        }

        val entityIds = entityResponses.stream()
                .sorted(Comparator.comparing(EntityResponse::getEntityKey))
                .map(EntityResponse::getEntityId)
                .map(Long::parseLong)
                .toList();

        val availableEntityPOList = entityService.listEntityPOById(entityIds);
        val availableEntityIds = availableEntityPOList.stream().map(EntityPO::getId).toList();
        val entityIdToPO = entityResponses.stream()
                .collect(Collectors.toMap(EntityResponse::getEntityId, Function.identity(), (v1, v2) -> v1));

        val startTime = entityExportRequest.getStartTimestamp() == null ? 0 : entityExportRequest.getStartTimestamp();
        val endTime = entityExportRequest.getEndTimestamp() == null ? System.currentTimeMillis() : entityExportRequest.getEndTimestamp();
        if (endTime <= startTime) {
            throw ServiceException.with(ErrorCode.PARAMETER_VALIDATION_FAILED)
                    .detailMessage("startTimestamp should be less than endTimestamp")
                    .build();
        }
        val zoneId = StringUtils.hasText(entityExportRequest.getTimeZone()) ? ZoneId.of(entityExportRequest.getTimeZone()) : ZoneId.systemDefault();
        val outputStream = httpServletResponse.getOutputStream();
        exporter.export(outputStream, i -> {
            val query = new GenericPageRequest();
            query.setPageNumber(i + 1);
            query.sort(new Sorts().desc(EntityHistoryPO.Fields.timestamp).asc(EntityHistoryPO.Fields.entityId));
            return entityValueService.historySearchSlice(availableEntityIds, startTime, endTime, query)
                    .stream()
                    .map(historyResponse -> {
                        val entityResponse = entityIdToPO.get(historyResponse.getEntityId());
                        if (entityResponse == null) {
                            return null;
                        }

                        val dateTime = getDateTime(historyResponse, zoneId);
                        val entityExportData = new EntityExportData();
                        entityExportData.setUpdateTime(dateTime);
                        entityExportData.setEntityName(entityResponse.getEntityName());
                        entityExportData.setEntityIdentifier(entityResponse.getEntityKey());
                        entityExportData.setIntegrationName(entityResponse.getIntegrationName());
                        entityExportData.setDeviceName(entityResponse.getDeviceName());

                        val value = String.valueOf(historyResponse.getValue());
                        @SuppressWarnings({"unchecked"})
                        val mappedValue = Optional.ofNullable(entityResponse.getEntityValueAttribute())
                                .map(v -> (Map<String, String>) v.get("enum"))
                                .map(v -> v.get(value))
                                .orElse(value);
                        entityExportData.setValue(mappedValue);

                        return entityExportData;
                    })
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(EntityExportData::getUpdateTime).reversed()
                            .thenComparing(EntityExportData::getEntityIdentifier))
                    .toList();
        });

        val nowDateTime = ZonedDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
        httpServletResponse.setHeader("Content-Type", "text/csv");
        httpServletResponse.setHeader("Content-Disposition", String.format("attachment; filename=EntityData_%s.csv", nowDateTime));
    }

}
