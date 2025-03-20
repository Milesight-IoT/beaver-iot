package com.milesight.beaveriot.user.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.permission.enums.MenuCode;
import com.milesight.beaveriot.permission.enums.OperationPermissionCode;
import com.milesight.beaveriot.user.constants.UserConstants;
import com.milesight.beaveriot.user.enums.MenuType;
import com.milesight.beaveriot.user.enums.TenantStatus;
import com.milesight.beaveriot.user.po.MenuPO;
import com.milesight.beaveriot.user.po.RolePO;
import com.milesight.beaveriot.user.po.TenantPO;
import com.milesight.beaveriot.user.repository.MenuRepository;
import com.milesight.beaveriot.user.repository.RoleRepository;
import com.milesight.beaveriot.user.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author loong
 */
@Service
public class TenantService {

    @Autowired
    TenantRepository tenantRepository;
    @Autowired
    RoleRepository roleRepository;
    @Autowired
    MenuRepository menuRepository;

    @Transactional(rollbackFor = Exception.class)
    public void createTenant(String tenantId, String tenantName, String domain, String timeZone) {
        TenantPO tenantPO = new TenantPO();
        tenantPO.setId(tenantId);
        tenantPO.setName(tenantName);
        tenantPO.setDomain(domain);
        tenantPO.setStatus(TenantStatus.ENABLE);
        tenantPO.setTimeZone(timeZone);
        tenantRepository.save(tenantPO);

        try {
            TenantContext.setTenantId(tenantId);

            RolePO rolePO = new RolePO();
            rolePO.setId(SnowflakeUtil.nextId());
            rolePO.setName(UserConstants.SUPER_ADMIN_ROLE_NAME);
            roleRepository.save(rolePO);

            List<MenuPO> menuPOS = new ArrayList<>();
            Arrays.stream(OperationPermissionCode.values()).forEach(operationPermissionCode -> {
                MenuCode parentMenu = operationPermissionCode.getParent();
                Long parentMenuId = null;
                if (parentMenu != null) {
                    parentMenuId = createTenantMenu(parentMenu, menuPOS);
                }
                MenuPO menuPO = new MenuPO();
                menuPO.setId(SnowflakeUtil.nextId());
                menuPO.setName(operationPermissionCode.getCode());
                menuPO.setCode(operationPermissionCode.getCode());
                menuPO.setType(MenuType.FUNCTION);
                menuPO.setParentId(parentMenuId);
                menuPOS.add(menuPO);
            });
            menuRepository.saveAll(menuPOS);
        }finally {
            TenantContext.clear();
        }
    }

    private Long createTenantMenu(MenuCode menuCode, List<MenuPO> menuPOS) {
        MenuCode parentMenu = menuCode.getParent();
        Long parentMenuId = null;
        if (parentMenu != null) {
            parentMenuId = createTenantMenu(parentMenu, menuPOS);
        }
        MenuPO menuPO = new MenuPO();
        menuPO.setId(SnowflakeUtil.nextId());
        menuPO.setName(menuCode.getCode());
        menuPO.setCode(menuCode.getCode());
        menuPO.setType(MenuType.MENU);
        menuPO.setParentId(parentMenuId);
        menuPOS.add(menuPO);
        return menuPO.getId();
    }

}
