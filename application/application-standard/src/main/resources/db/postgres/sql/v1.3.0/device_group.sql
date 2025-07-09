--liquibase formatted sql

--changeset simon:device_group_v1.2.3_20250625_151900
CREATE TABLE t_device_group (
    id              BIGINT          NOT NULL,
    name            VARCHAR(1024)   NOT NULL,
    tenant_id       VARCHAR(255)    NOT NULL,
    created_at      BIGINT          DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_device_group_tenant_id_name UNIQUE (tenant_id, name)
);

CREATE TABLE t_device_group_mapping (
    id              BIGINT          NOT NULL,
    group_id        BIGINT          NOT NULL,
    device_id       BIGINT          NOT NULL,
    tenant_id       VARCHAR(255)    NOT NULL,
    created_at      BIGINT          DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_device_group_mapping_group_id_device_id UNIQUE (group_id, device_id)
);

CREATE INDEX idx_device_group_mapping_device_id ON t_device_group_mapping (device_id);
CREATE INDEX idx_device_group_mapping_tenant_id ON t_device_group_mapping (tenant_id);
