package com.milesight.beaveriot.context.api;


import com.milesight.beaveriot.context.integration.model.Credentials;

import java.util.List;
import java.util.Optional;

public interface CredentialsServiceProvider {

    void addCredentials(Credentials credentials);

    void batchDeleteCredentials(List<Long> ids);

    Optional<Credentials> getCredentials(String tenantId, String credentialType);

    Credentials getOrCreateDefaultCredentials(String tenantId, String credentialType);

    Credentials getOrCreateCredentials(String tenantId, String credentialType, String username);

    Optional<Credentials> getCredentials(String tenantId, Long id);

    Optional<Credentials> getCredentials(String tenantId, String credentialType, String accessKey);

}
