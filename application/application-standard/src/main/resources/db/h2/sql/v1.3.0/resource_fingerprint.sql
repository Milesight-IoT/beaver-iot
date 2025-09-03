--liquibase formatted sql

--changeset pandalxb:resource_fingerprint_v1.3.0_20250903_161200
CREATE TABLE `t_resource_fingerprint`
(
    id                      BIGINT       NOT NULL,
    type                    VARCHAR(255) NOT NULL,
    integration             VARCHAR(127) NOT NULL,
    hash                    VARCHAR(128) NOT NULL,
    created_at              BIGINT       NOT NULL,
    updated_at              BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_resource_fingerprint_type_integration UNIQUE (type, integration)
);