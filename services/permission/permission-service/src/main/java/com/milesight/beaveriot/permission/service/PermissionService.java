package com.milesight.beaveriot.permission.service;

import com.milesight.beaveriot.base.enums.ErrorCode;
import com.milesight.beaveriot.base.exception.ServiceException;
import com.milesight.beaveriot.context.security.SecurityUserContext;
import com.milesight.beaveriot.permission.dto.PermissionDTO;
import com.milesight.beaveriot.permission.enums.DataPermissionType;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.permission.facade.IPermissionFacade;
import com.milesight.beaveriot.permission.helper.TemporaryPermission;
import com.milesight.beaveriot.user.dto.MenuDTO;
import com.milesight.beaveriot.user.facade.IUserFacade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * PermissionService class.
 *
 * @author simon
 * @date 2025/9/10
 */
@Service
public class PermissionService implements IPermissionFacade {

    @Lazy
    @Autowired
    IUserFacade userFacade;

    @Lazy
    @Autowired
    EntityPermissionService entityPermissionService;

    @Lazy
    @Autowired
    DashboardPermissionService dashboardPermissionService;

    @Lazy
    @Autowired
    DevicePermissionService devicePermissionService;

    @Lazy
    @Autowired
    WorkflowPermissionService workflowPermissionService;

    private Long getContextUserId() {
        Long userId = SecurityUserContext.getUserId();
        if (userId == null) {
            throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user not logged in").build();
        }

        return userId;
    }

    @Override
    public void checkMenuPermission(OperationPermissionCode ...codes) {
        boolean hasPermission = hasMenuPermission(codes);
        if (!hasPermission) {
            throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user does not have permission").build();
        }
    }

    @Override
    public void checkAdminPermission() {
        Long userId = getContextUserId();
        boolean isSuperAdmin = userFacade.isSuperAdmin(userId);
        if (!isSuperAdmin) {
            throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user does not have permission").build();
        }
    }

    @Override
    public boolean hasMenuPermission(OperationPermissionCode ...codes) {
        if (codes == null || codes.length == 0) {
            return true;
        }

        if (TemporaryPermission.contains(codes)) {
            return true;
        }

        List<MenuDTO> menuDTOList = userFacade.getMenusByUserId(getContextUserId());
        if (menuDTOList == null || menuDTOList.isEmpty()) {
            return false;
        }

        Set<String> operationPermissionCodes = Arrays.stream(codes)
                .map(OperationPermissionCode::getCode)
                .collect(Collectors.toSet());
        return menuDTOList.stream().anyMatch(menuDTO -> operationPermissionCodes.contains(menuDTO.getMenuCode()));
    }

    public PermissionDTO getDataPermission(DataPermissionType type) {
        Long userId = getContextUserId();
        PermissionDTO permissionDTO = switch (type) {
            case ENTITY -> entityPermissionService.getEntityPermission(userId);
            case DEVICE -> devicePermissionService.getDevicePermission(userId);
            case DASHBOARD -> dashboardPermissionService.getDashboardPermission(userId);
            case WORKFLOW -> workflowPermissionService.getWorkflowPermission(userId);
        };

        if (permissionDTO == null) {
            throw ServiceException.with(ErrorCode.PARAMETER_SYNTAX_ERROR).detailMessage("unknown data permission type").build();
        }

        var tempIds = TemporaryPermission.getResourceIds(type);
        if (!tempIds.isEmpty()) {
            var newIds = new ArrayList<>(tempIds);
            var ids = permissionDTO.getIds();
            if (ids != null && !ids.isEmpty()) {
                newIds.addAll(ids);
            }
            permissionDTO.setIds(newIds);
        }

        return permissionDTO;
    }

    @Override
    public void checkDataPermission(DataPermissionType type, String id) {
        PermissionDTO permissionDTO = getDataPermission(type);
        if (permissionDTO.isHaveAllPermissions()) {
            return;
        }

        if (!permissionDTO.getIds().contains(id)) {
            throw ServiceException.with(ErrorCode.FORBIDDEN_PERMISSION).detailMessage("user does not have permission").build();
        }
    }

}
