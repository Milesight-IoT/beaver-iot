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
 * @date 2024/10/14 9:43
 */
@Data
@Table(name = "oauth2_authorization")
@Entity
@FieldNameConstants
public class OAuth2AuthorizationPO {

    @Id
    private String id;
    private String registeredClientId;
    private String principalName;
    private String authorizationGrantType;

    @Column(length = 1000)
    private String authorizedScopes;
    @Column(columnDefinition = "BLOB")
    private String attributes;
    @Column(length = 500)
    private String state;
    @Column(columnDefinition = "BLOB")
    private String authorizationCodeValue;
    private Timestamp authorizationCodeIssuedAt;
    private Timestamp authorizationCodeExpiresAt;
    @Column(columnDefinition = "BLOB")
    private String authorizationCodeMetadata;
    @Column(columnDefinition = "BLOB")
    private String accessTokenValue;
    private Timestamp accessTokenIssuedAt;
    private Timestamp accessTokenExpiresAt;
    @Column(columnDefinition = "BLOB")
    private String accessTokenMetadata;
    private String accessTokenType;
    @Column(length = 1000)
    private String accessTokenScopes;
    @Column(columnDefinition = "BLOB")
    private String oidcIdTokenValue;
    private Timestamp oidcIdTokenIssuedAt;
    private Timestamp oidcIdTokenExpiresAt;
    @Column(columnDefinition = "BLOB")
    private String oidcIdTokenMetadata;
    @Column(columnDefinition = "BLOB")
    private String refreshTokenValue;
    private Timestamp refreshTokenIssuedAt;
    private Timestamp refreshTokenExpiresAt;
    @Column(columnDefinition = "BLOB")
    private String refreshTokenMetadata;
    @Column(columnDefinition = "BLOB")
    private String userCodeValue;
    private Timestamp userCodeIssuedAt;
    private Timestamp userCodeExpiresAt;
    @Column(columnDefinition = "BLOB")
    private String userCodeMetadata;
    @Column(columnDefinition = "BLOB")
    private String deviceCodeValue;
    private Timestamp deviceCodeIssuedAt;
    private Timestamp deviceCodeExpiresAt;
    @Column(columnDefinition = "BLOB")
    private String deviceCodeMetadata;

}
