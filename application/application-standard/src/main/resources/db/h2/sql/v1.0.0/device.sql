--liquibase formatted sql

--changeset simon:device_v1.0.0_20241024_095400
CREATE TABLE t_device
(
    id              BIGINT       NOT NULL,
    "key"           VARCHAR(512) NOT NULL,
    name            VARCHAR(255) NOT NULL,
    integration     VARCHAR(127) NOT NULL,
    identifier      VARCHAR(127) NOT NULL,
    additional_data VARCHAR(1024) DEFAULT NULL,
    created_at      BIGINT        DEFAULT NULL,
    updated_at      BIGINT        DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE ("key"),
    UNIQUE (integration, identifier)
);
