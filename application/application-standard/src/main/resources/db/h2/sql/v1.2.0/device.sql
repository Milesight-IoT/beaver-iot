--liquibase formatted sql

--changeset loong:device_v1.2.0_20250319_155400
EXECUTE IMMEDIATE 'ALTER TABLE t_device DROP CONSTRAINT ' ||
                  (SELECT CONSTRAINT_NAME
                   FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                   WHERE TABLE_NAME = 'T_DEVICE' AND CONSTRAINT_TYPE = 'UNIQUE'
    LIMIT 1);

alter table t_device add constraint uk_device_key unique("key", tenant_id);

--changeset loong:device_v1.2.0_20250319_160000
EXECUTE IMMEDIATE 'ALTER TABLE t_device DROP CONSTRAINT ' ||
                  (SELECT CONSTRAINT_NAME
                   FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                   WHERE TABLE_NAME = 'T_DEVICE' AND CONSTRAINT_TYPE = 'UNIQUE'
                     AND CONSTRAINT_NAME like 'CONSTRAINT%'
    LIMIT 1);

alter table t_device add constraint uk_device_integration_identifier unique(integration, identifier, tenant_id);

--changeset pandalxb:device_v1.2.0_20250514_083900
ALTER TABLE `t_device`
    ADD COLUMN template VARCHAR(512);
CREATE INDEX idx_device_template ON `t_device` (template);