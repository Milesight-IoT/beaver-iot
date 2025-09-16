package com.milesight.beaveriot.devicetemplate.support;

import com.milesight.beaveriot.context.integration.model.DeviceTemplate;
import com.milesight.beaveriot.context.integration.model.DeviceTemplateBuilder;
import com.milesight.beaveriot.devicetemplate.po.DeviceTemplatePO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DeviceTemplateConverter {
    public DeviceTemplate convertPO(DeviceTemplatePO deviceTemplatePO) {
        return this.convertPO(Collections.singletonList(deviceTemplatePO)).get(0);
    }

    public List<DeviceTemplate> convertPO(List<DeviceTemplatePO> deviceTemplatePOList) {
        return deviceTemplatePOList.stream().map((deviceTemplatePO -> {
            DeviceTemplate deviceTemplate = new DeviceTemplateBuilder(deviceTemplatePO.getIntegration())
                    .name(deviceTemplatePO.getName())
                    .content(deviceTemplatePO.getContent())
                    .description(deviceTemplatePO.getDescription())
                    .identifier(deviceTemplatePO.getIdentifier())
                    .id(deviceTemplatePO.getId())
                    .additional(deviceTemplatePO.getAdditionalData())
                    .vendor(deviceTemplatePO.getVendor())
                    .model(deviceTemplatePO.getModel())
                    .blueprintLibraryId(deviceTemplatePO.getBlueprintLibraryId())
                    .blueprintLibraryVersion(deviceTemplatePO.getBlueprintLibraryVersion())
                    .build();
            deviceTemplate.setCreatedAt(deviceTemplatePO.getCreatedAt());
            deviceTemplate.setUpdatedAt(deviceTemplatePO.getUpdatedAt());
            return deviceTemplate;
        })).collect(Collectors.toList());
    }
}
