package com.milesight.beaveriot.rule.manager.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.base.utils.StringUtils;
import com.milesight.beaveriot.device.facade.IDeviceBlueprintMappingFacade;
import com.milesight.beaveriot.rule.model.flow.config.RuleFlowConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

/**
 * DeviceWorkflowService class.
 *
 * @author simon
 * @date 2025/9/17
 */
@Service
public class DeviceWorkflowService {
    @Autowired
    IDeviceBlueprintMappingFacade deviceBlueprintMappingFacade;

    public Long fetchDeviceIdFromWorkflowMetadata(@Nullable RuleFlowConfig ruleFlowConfig) {
        if (ruleFlowConfig == null
                || ruleFlowConfig.getMetadata() == null
                || StringUtils.isEmpty(ruleFlowConfig.getMetadata().getBlueprintId())
        ) {
            return null;
        }

        Long blueprintId = Long.valueOf(ruleFlowConfig.getMetadata().getBlueprintId());
        Long relatedDeviceId = deviceBlueprintMappingFacade.getDeviceIdByBlueprintId(blueprintId);
        if (relatedDeviceId == null) {
            throw ServiceException.with(ErrorCode.DATA_NO_FOUND.getErrorCode(), "Cannot find device for blueprint: " + blueprintId).build();
        }

        return relatedDeviceId;
    }

    public Long fetchDeviceIdFromExistsBlueprintWorkflow(Long workflowId) {
        // TODO: find device id from workflow -> blueprint -> device
        return null;
    }
}
