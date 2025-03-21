package com.milesight.beaveriot.context.api;


import com.milesight.beaveriot.context.integration.model.Credentials;

import java.util.List;
import java.util.Optional;

public interface CredentialsServiceProvider {

    void addCredentials(Credentials credentials);

    void batchDeleteCredentials(List<Long> ids);

    Optional<Credentials> getCredentials(String credentialType);

    Credentials getOrCreateDefaultCredentials(String credentialType);

    Credentials getOrCreateCredentials(String credentialType, String username);

    Optional<Credentials> getCredentials(Long id);

    Optional<Credentials> getCredentials(String credentialType, String accessKey);

}
