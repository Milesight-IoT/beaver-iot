package com.milesight.beaveriot.permission.facade;

import com.milesight.beaveriot.permission.enums.DataPermissionType;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;

/**
 * IPermissionFacade
 *
 * @author simon
 * @date 2025/9/10
 */
public interface IPermissionFacade {
    void checkMenuPermission(OperationPermissionCode ...codes);

    boolean hasMenuPermission(OperationPermissionCode ...codes);

    void checkDataPermission(DataPermissionType type, String id);

    void checkAdminPermission();
}
