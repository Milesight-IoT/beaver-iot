package com.milesight.beaveriot.credentials.repository;

import com.milesight.beaveriot.credentials.po.CredentialsPO;
import com.milesight.beaveriot.data.jpa.repository.BaseJpaRepository;
import com.milesight.beaveriot.permission.aspect.Tenant;

import java.util.Optional;

@Tenant
public interface CredentialsRepository extends BaseJpaRepository<CredentialsPO, Long> {

    Optional<CredentialsPO> findFirstByCredentialsTypeAndAccessKey(String credentialsType, String accessKey);

}
