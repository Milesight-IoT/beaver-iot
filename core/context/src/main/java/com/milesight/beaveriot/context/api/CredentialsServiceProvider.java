package com.milesight.beaveriot.context.api;


import com.milesight.beaveriot.context.integration.enums.CredentialsType;
import com.milesight.beaveriot.context.integration.model.Credentials;

import java.util.List;
import java.util.Optional;

public interface CredentialsServiceProvider {

    void addCredentials(Credentials credentials);

    void batchDeleteCredentials(List<Long> ids);

    Optional<Credentials> getCredentials(String credentialType);

    Optional<Credentials> getCredentials(CredentialsType credentialType);

    Credentials getOrCreateCredentials(String credentialType);

    Credentials getOrCreateCredentials(String credentialType, String password);

    Credentials getOrCreateCredentials(CredentialsType credentialType);

    Credentials getOrCreateCredentials(CredentialsType credentialType, String password);

    Credentials getOrCreateCredentials(String credentialType, String username, String password);

    Credentials getOrCreateCredentials(CredentialsType credentialType, String username, String password);

    Optional<Credentials> getCredentials(Long id);

    Optional<Credentials> getCredentials(String credentialType, String accessKey);

    Optional<Credentials> getCredentials(CredentialsType credentialType, String accessKey);

}
