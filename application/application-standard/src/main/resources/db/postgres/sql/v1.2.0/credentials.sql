--liquibase formatted sql

--changeset Maglitch65:credentials_v1.2.0_20250224_103000
CREATE TABLE "t_credentials"
(
    id                      BIGINT PRIMARY KEY,
    tenant_id               VARCHAR(255) NOT NULL,
    credentials_type        VARCHAR(255) NOT NULL,
    access_key              VARCHAR(255) NOT NULL,
    access_secret           VARCHAR(255) NOT NULL,
    description             VARCHAR(2048)    DEFAULT NULL,
    additional_data         VARCHAR(1048576) DEFAULT NULL,
    cryptographic_algorithm VARCHAR(255)     DEFAULT NULL,
    editable                BOOLEAN      NOT NULL,
    visible                 BOOLEAN      NOT NULL,
    created_at              BIGINT       NOT NULL,
    created_by              VARCHAR(255)     DEFAULT NULL,
    updated_at              BIGINT       NOT NULL,
    updated_by              VARCHAR(255)     DEFAULT NULL,
    CONSTRAINT uk_tenant_id_credentials_type_access_key UNIQUE (tenant_id, credentials_type, access_key)
);

