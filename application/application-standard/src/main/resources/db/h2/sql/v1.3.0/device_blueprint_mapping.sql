--liquibase formatted sql

--changeset pandalxb:device_blueprint_mapping_v1.3.0_20250909_144500
CREATE TABLE `t_device_blueprint_mapping` (
    id             BIGINT       NOT NULL,
    device_id      BIGINT       NOT NULL,
    blueprint_id   BIGINT       NOT NULL,
    tenant_id      VARCHAR(255) NOT NULL,
    created_at     BIGINT       NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_device_blueprint_mapping_device_id_blueprint_id UNIQUE (device_id, blueprint_id)
);

CREATE INDEX idx_device_blueprint_mapping_tenant_id ON `t_device_blueprint_mapping` (tenant_id);

--changeset simon:device_blueprint_mapping_v1.3.0_20250924_163000
CREATE INDEX idx_device_blueprint_mapping_blueprint_id ON `t_device_blueprint_mapping` (blueprint_id);
