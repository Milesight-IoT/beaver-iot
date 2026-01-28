--liquibase formatted sql

-- changeset pandalxb:authentication_v1.3.1_20260128_141200
CREATE INDEX idx_principal_name_access_token_expires_at ON oauth2_authorization (principal_name, access_token_expires_at);

ALTER TABLE oauth2_authorization
    ADD CONSTRAINT uk_access_token_value UNIQUE (access_token_value);

ALTER TABLE oauth2_authorization
    ADD CONSTRAINT uk_refresh_token_value UNIQUE (refresh_token_value);
