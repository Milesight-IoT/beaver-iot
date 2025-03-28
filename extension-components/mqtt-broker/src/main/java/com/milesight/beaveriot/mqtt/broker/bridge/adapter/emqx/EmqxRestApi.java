package com.milesight.beaveriot.mqtt.broker.bridge.adapter.emqx;

import com.milesight.beaveriot.base.utils.JsonUtils;
import com.milesight.beaveriot.mqtt.broker.bridge.adapter.emqx.model.EmqxAcl;
import lombok.*;
import lombok.extern.slf4j.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class EmqxRestApi {

    private final String restApiEndpoint;

    private final HttpClient httpClient;


    @SneakyThrows
    public boolean ensureAuthenticator() {
        val response = httpClient.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString("{\"password_hash_algorithm\":{\"name\":\"sha256\",\"salt_position\":\"prefix\"},\"mechanism\":\"password_based\",\"backend\":\"built_in_database\",\"user_id_type\":\"username\"}"))
                        .uri(new URI(String.format("%s/api/v5/authentication", restApiEndpoint)))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            log.error("emqx init authenticator failed, status code: {}, body: {}", response.statusCode(), response.body());
            return false;
        }
        return true;
    }

    @SneakyThrows
    public boolean ensureAuthorizationSource() {
        val response = httpClient.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString("{\"type\":\"built_in_database\"}"))
                        .uri(new URI(String.format("%s/api/v5/authorization/sources", restApiEndpoint)))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            log.error("emqx init authorization source failed, status code: {}, body: {}", response.statusCode(), response.body());
            return false;
        }
        return true;
    }

    public boolean ensureMqttAdminUser(String username, String password) {
        if (checkUserExist(username)) {
            return true;
        }
        return addUser(username, password, true);
    }

    @SneakyThrows
    public boolean addUser(String username, String password, boolean isAdmin) {
        val response = httpClient.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(String.format("{\"user_id\":\"%s\",\"password\":\"%s\",\"is_superuser\":%b}", username, password, isAdmin)))
                        .uri(new URI(String.format("%s/api/v5/authentication/password_based:built_in_database/users", restApiEndpoint)))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            log.error("emqx add user '{}' failed, status code: {}, body: {}", username, response.statusCode(), response.body());
            return false;
        }
        return true;
    }

    @SneakyThrows
    public boolean deleteUser(String username) {
        val response = httpClient.send(HttpRequest.newBuilder()
                        .DELETE()
                        .uri(new URI(String.format("%s/api/v5/authentication/password_based:built_in_database/users/%s", restApiEndpoint, username)))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            log.error("emqx delete user '{}' failed, status code: {}, body: {}", username, response.statusCode(), response.body());
            return false;
        }
        return true;
    }

    @SneakyThrows
    public boolean addAclRule(EmqxAcl emqxAcl) {
        val requestBodyString = JsonUtils.toJSON(List.of(emqxAcl));
        val response = httpClient.send(HttpRequest.newBuilder()
                        .POST(HttpRequest.BodyPublishers.ofString(requestBodyString))
                        .uri(new URI(String.format("%s/api/v5/authorization/sources/built_in_database/rules/users", restApiEndpoint)))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            log.error("emqx add acl for user '{}' failed, status code: {}, body: {}", emqxAcl.getUsername(), response.statusCode(), response.body());
        }
        return true;
    }

    @SneakyThrows
    public boolean deleteAclRule(String username) {
        val response = httpClient.send(HttpRequest.newBuilder()
                        .DELETE()
                        .uri(new URI(String.format("%s/api/v5/authorization/sources/built_in_database/rules/users/%s", restApiEndpoint, username)))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            log.error("emqx delete acl for user '{}' failed, status code: {}, body: {}", username, response.statusCode(), response.body());
            return false;
        }
        return true;
    }

    @SneakyThrows
    public boolean checkUserExist(String username) {
        val response = httpClient.send(HttpRequest.newBuilder()
                        .GET()
                        .uri(new URI(String.format("%s/api/v5/authentication/password_based:built_in_database/users/%s", restApiEndpoint, username)))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) {
            // not exist
            return false;
        } else if (response.statusCode() >= 400) {
            log.error("emqx check user '{}' exist failed, status code: {}, body: {}", username, response.statusCode(), response.body());
            return false;
        }
        log.info("emqx user found: {}", response.body());
        return true;
    }


}
