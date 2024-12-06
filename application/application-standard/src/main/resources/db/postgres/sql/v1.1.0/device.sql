--liquibase formatted sql

--changeset loong:device_v1.1.0_20241122_164400
alter table "t_device"
    add column tenant_id bigint not null default 1, add column user_id bigint;
CREATE INDEX idx_device_tenant_id ON "t_device" (tenant_id);