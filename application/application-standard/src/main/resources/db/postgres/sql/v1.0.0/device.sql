--liquibase formatted sql

--changeset simon:device_v1.0.0_20241024_095400
CREATE TABLE t_device
(
    id              BIGINT       NOT NULL,                                       -- Device ID
    key             VARCHAR(512) NOT NULL,                                       -- Device Key
    name            VARCHAR(255) NOT NULL,                                       -- Device Name
    integration     VARCHAR(127) NOT NULL,                                       -- Integration Id
    identifier      VARCHAR(127) NOT NULL,                                       -- Device External Identifier
    additional_data VARCHAR(1024) DEFAULT NULL,                                  -- Device Additional Data
    created_at      BIGINT        DEFAULT NULL,                                  -- Create Timestamp
    updated_at      BIGINT        DEFAULT NULL,                                  -- Update Timestamp
    PRIMARY KEY (id),
    CONSTRAINT uk_device_key UNIQUE (key),                                       -- Unique Index for Device Key
    CONSTRAINT uk_device_integration_identifier UNIQUE (integration, identifier) -- Unique Index for Device Integration Identifier
);
