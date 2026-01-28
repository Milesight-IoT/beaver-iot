--liquibase formatted sql

-- changeset pandalxb:authentication_v1.3.1_20260128_141200
CREATE INDEX idx_principal_name_access_token_expires_at ON oauth2_authorization (principal_name, access_token_expires_at);

ALTER TABLE oauth2_authorization ADD COLUMN access_token_value_new VARCHAR(4096);
UPDATE oauth2_authorization SET access_token_value_new = UTF8TOSTRING(access_token_value);
ALTER TABLE oauth2_authorization DROP COLUMN access_token_value;
ALTER TABLE oauth2_authorization RENAME COLUMN access_token_value_new TO access_token_value;

ALTER TABLE oauth2_authorization ADD COLUMN refresh_token_value_new VARCHAR(4096);
UPDATE oauth2_authorization SET refresh_token_value_new = UTF8TOSTRING(refresh_token_value);
ALTER TABLE oauth2_authorization DROP COLUMN refresh_token_value;
ALTER TABLE oauth2_authorization RENAME COLUMN refresh_token_value_new TO refresh_token_value;

ALTER TABLE oauth2_authorization
    ADD CONSTRAINT uk_access_token_value UNIQUE (access_token_value);

ALTER TABLE oauth2_authorization
   ADD CONSTRAINT uk_refresh_token_value UNIQUE (refresh_token_value);