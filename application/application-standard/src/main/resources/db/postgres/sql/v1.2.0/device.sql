--liquibase formatted sql

--changeset loong:device_v1.2.0_20250319_155400
alter table t_device
drop constraint uk_device_key;

alter table t_device add constraint uk_device_key unique(key, tenant_id);