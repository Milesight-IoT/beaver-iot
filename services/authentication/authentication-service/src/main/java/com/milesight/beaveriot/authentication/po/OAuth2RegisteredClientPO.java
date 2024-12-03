package com.milesight.beaveriot.authentication.po;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

import java.sql.Timestamp;

/**
 * @author loong
 * @date 2024/10/14 9:40
 */
@Data
@Table(name = "oauth2_registered_client")
@Entity
@FieldNameConstants
public class OAuth2RegisteredClientPO {

    @Id
    private String id;
    private String clientId;
    private Timestamp clientIdIssuedAt;
    private String clientSecret;
    private Timestamp clientSecretExpiresAt;
    private String clientName;

    @Column(length = 1000)
    private String clientAuthenticationMethods;

    @Column(length = 1000)
    private String authorizationGrantTypes;

    @Column(length = 1000)
    private String redirectUris;

    @Column(length = 1000)
    private String postLogoutRedirectUris;

    @Column(length = 1000)
    private String scopes;

    @Column(length = 2000)
    private String clientSettings;

    @Column(length = 2000)
    private String tokenSettings;

}
