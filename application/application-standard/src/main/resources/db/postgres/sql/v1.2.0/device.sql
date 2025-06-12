--liquibase formatted sql

--changeset loong:device_v1.2.0_20250319_155400
alter table t_device
drop constraint uk_device_key;

alter table t_device add constraint uk_device_key unique(key, tenant_id);

--changeset loong:device_v1.2.0_20250319_160000
alter table t_device
drop constraint uk_device_integration_identifier;

alter table t_device add constraint uk_device_integration_identifier unique(integration, identifier, tenant_id);

--changeset pandalxb:device_v1.2.0_20250514_083900
ALTER TABLE t_device
    ADD COLUMN template VARCHAR(512);
CREATE INDEX idx_device_template ON t_device (template);