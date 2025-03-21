package com.milesight.beaveriot.credentials.repository;

import com.milesight.beaveriot.credentials.po.CredentialsPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;

import java.util.Optional;

@Tenant
public interface CredentialsRepository extends BaseJpaRepository<CredentialsPO, Long> {

    @Tenant(enable = false)
    Optional<CredentialsPO> findFirstByTenantIdAndCredentialsTypeAndAccessKey(String tenantId, String credentialsType, String accessKey);

    @Tenant(enable = false)
    Optional<CredentialsPO> findFirstByTenantIdAndId(String tenantId, Long id);

    @Tenant(enable = false)
    Optional<CredentialsPO> findFirstByTenantIdAndCredentialsType(String tenantId, String credentialsType);

}
