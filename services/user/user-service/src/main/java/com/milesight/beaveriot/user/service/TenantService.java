package com.milesight.beaveriot.user.service;

import com.milesight.beaveriot.base.utils.snowflake.SnowflakeUtil;
import com.milesight.beaveriot.context.api.TenantServiceProvider;
import com.milesight.beaveriot.context.security.TenantContext;
import com.milesight.beaveriot.user.constants.UserConstants;
import com.milesight.beaveriot.user.enums.TenantStatus;
import com.milesight.beaveriot.user.po.RolePO;
import com.milesight.beaveriot.user.po.TenantPO;
import com.milesight.beaveriot.user.repository.RoleRepository;
import com.milesight.beaveriot.user.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author loong
 */
@Service
public class TenantService implements TenantServiceProvider {

    @Autowired
    TenantRepository tenantRepository;
    @Autowired
    RoleRepository roleRepository;

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
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    public void runWithAllTenants(Runnable runnable) {
        tenantRepository.findAll().forEach(tenant -> {
            try {
                TenantContext.setTenantId(tenant.getId());
                runnable.run();
            } finally {
                TenantContext.clear();
            }
        });
    }
}
